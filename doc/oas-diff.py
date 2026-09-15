"""Normalising comparison of two OpenAPI documents (Javalin 3.0.3 baseline vs Ktor 3.1.1 output).

Compares the things the generated Angular client depends on:
paths -> methods -> operationId, tags, summary, parameters (name/in/required/type/format),
request body content types + schema refs/types, response codes + content types + schema refs/types,
component schema names, property names, property types, nullability, required lists, enum values.
"""
import json
import sys


def load(p):
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def norm_schema(s):
    """Normalise a (possibly inline) schema to a comparable tuple: (type/ref, format, nullable, items)."""
    if s is None:
        return None
    if "$ref" in s:
        return ("ref", s["$ref"].split("/")[-1], bool(s.get("nullable", False)))
    # 3.1 nullable encodings
    nullable = bool(s.get("nullable", False))
    t = s.get("type")
    if isinstance(t, list):
        nullable = nullable or "null" in t
        t = [x for x in t if x != "null"]
        t = t[0] if len(t) == 1 else tuple(t)
    for key in ("oneOf", "anyOf"):
        if key in s:
            branches = [b for b in s[key] if not (b.get("type") == "null")]
            if len(branches) < len(s[key]):
                nullable = True
            if len(branches) == 1:
                inner = norm_schema(branches[0])
                return inner[:-1] + (inner[-1] or nullable,) if inner else None
    if t == "array":
        return ("array", norm_schema(s.get("items")), nullable)
    if t == "object" and "additionalProperties" in s and isinstance(s["additionalProperties"], dict):
        return ("map", norm_schema(s["additionalProperties"]), nullable)
    if t == "object" and "properties" in s:
        return ("inline-object", tuple(sorted(s["properties"].keys())), nullable)
    fmt = s.get("format")
    if "enum" in s:
        return ("enum", tuple(s["enum"]), nullable)
    return (t, fmt, nullable)


def norm_content(c):
    if not c:
        return None
    return {ct: norm_schema(v.get("schema")) for ct, v in c.items()}


def norm_op(op):
    return {
        "operationId": op.get("operationId"),
        "tags": tuple(op.get("tags", [])),
        "summary": op.get("summary"),
        "parameters": sorted(
            (p["name"], p["in"], bool(p.get("required", False)), norm_schema(p.get("schema")), p.get("description"))
            for p in op.get("parameters", [])
        ),
        "requestBody": (
            {"required": op["requestBody"].get("required", False), "content": norm_content(op["requestBody"].get("content"))}
            if "requestBody" in op else None
        ),
        "responses": {code: norm_content(r.get("content")) for code, r in op.get("responses", {}).items()},
    }


def norm_component(s):
    if "enum" in s:
        return {"enum": tuple(s["enum"])}
    props = s.get("properties", {})
    return {
        "properties": {k: norm_schema(v) for k, v in props.items()},
        "required": tuple(sorted(s.get("required", []))),
    }


def compare(a, b, path=""):
    diffs = []
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(set(a) | set(b)):
            if k not in a:
                diffs.append("%s/%s: only in NEW: %r" % (path, k, b[k]))
            elif k not in b:
                diffs.append("%s/%s: only in OLD: %r" % (path, k, a[k]))
            else:
                diffs += compare(a[k], b[k], "%s/%s" % (path, k))
    elif a != b:
        diffs.append("%s:\n    OLD %r\n    NEW %r" % (path, a, b))
    return diffs


def main(old_path, new_path):
    old, new = load(old_path), load(new_path)
    print("OLD openapi", old["openapi"], "| NEW openapi", new["openapi"])
    ops_old = {"%s %s" % (m.upper(), p): norm_op(op) for p, item in old["paths"].items() for m, op in item.items()}
    ops_new = {"%s %s" % (m.upper(), p): norm_op(op) for p, item in new["paths"].items() for m, op in item.items()}
    print("operations: OLD %d, NEW %d" % (len(ops_old), len(ops_new)))
    comp_old = {k: norm_component(v) for k, v in old["components"]["schemas"].items()}
    comp_new = {k: norm_component(v) for k, v in new["components"]["schemas"].items()}
    print("schemas: OLD %d, NEW %d" % (len(comp_old), len(comp_new)))
    print("securitySchemes OLD", old["components"].get("securitySchemes"), "NEW", new["components"].get("securitySchemes"))
    diffs = compare(ops_old, ops_new, "paths") + compare(comp_old, comp_new, "schemas")
    if not diffs:
        print("NO SEMANTIC DIFFERENCES")
    else:
        print("%d differences:" % len(diffs))
        for d in diffs:
            print(" -", d)
    return 0 if not diffs else 1


if __name__ == "__main__":
    sys.exit(main(sys.argv[1], sys.argv[2]))
