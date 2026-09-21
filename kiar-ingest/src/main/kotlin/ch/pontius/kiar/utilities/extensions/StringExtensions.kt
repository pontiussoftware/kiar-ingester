package ch.pontius.kiar.utilities.extensions

import ch.pontius.kiar.api.model.status.ErrorStatusException

/**
 * Adds a suffix to a string if it does not already end with it.
 *
 * @param suffix The suffix to add.
 * @return The string with the suffix.
 */
fun String.withSuffix(suffix: String): String = if (this.endsWith(suffix)) this else "$this$suffix"

/**
 * Returns true if this [String] can safely be used as a single path segment (a file or directory name).
 *
 * Rejects blank names, names containing path separators or control characters, the special names '.' and '..', and
 * names longer than 255 characters. Spaces and other printable characters are allowed, so existing names keep working.
 */
fun String.isSafePathSegment(): Boolean =
    this.isNotBlank() && this.length <= 255 && this != "." && this != ".." && this.none { it == '/' || it == '\\' || it.isISOControl() }

/**
 * Ensures that this [String] can safely be used as a single path segment; throws an [ErrorStatusException] (400) otherwise.
 *
 * @param label A label for the value used in the error message (e.g. "participant name").
 * @return This [String].
 */
fun String.requireSafePathSegment(label: String): String {
    if (!this.isSafePathSegment()) {
        throw ErrorStatusException(400, "Invalid $label '$this': it must not be empty, contain path separators or be '.' or '..'.")
    }
    return this
}
