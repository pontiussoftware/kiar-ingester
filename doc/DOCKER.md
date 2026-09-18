# Running KIAR Ingest with Docker

The image contains the Kotlin backend and the Angular dashboard in one process. Everything the
application reads or writes lives below `/config` and `/data` inside the container, so the host folders
the tools should work with are simply mapped onto those paths.

## Quick start

```bash
cp .env.example .env          # adjust host folders, port and UID/GID
docker compose up -d --build
```

The dashboard is then available at `http://localhost:7070` (or the port set in `KIAR_PORT`).

On the first start the container generates `config.json` in the mapped config folder from the
`KIAR_*` environment variables. Afterwards that file is the single source of truth; edit it and restart
the container to change settings. A ready-made example is in `configurations/docker-config.json`.

## Folder mappings

| Container path  | Purpose                                                                                   | `.env` variable    |
|-----------------|-------------------------------------------------------------------------------------------|--------------------|
| `/config`       | `config.json`                                                                             | `KIAR_CONFIG_DIR`  |
| `/data/db`      | SQLite database (`kiar.db`)                                                               | `KIAR_DB_DIR`      |
| `/data/ingest`  | Uploaded KIAR files and watched job template sources, one sub-folder per participant     | `KIAR_INGEST_DIR`  |
| `/data/logs`    | Application log (`kiar.log`, rolled at 100 MB)                                            | `KIAR_LOG_DIR`     |
| `/data/media`   | Default target for image deployments                                                     | `KIAR_MEDIA_DIR`   |

Two things are configured at runtime in the dashboard and must therefore refer to **container paths**:

- **Image deployments** (Apache Solr configuration, "Image Deployments" section): use `/data/media`
  or any other folder you mapped in.
- **Image sources**: the image value parser resolves paths found in the metadata (optionally rewritten
  by the mapping's search/replace parameters). Map the host share under the same path the mapping
  produces, read-only, e.g. `- /mnt/kimmedia:/mnt/kimmedia:ro` in `docker-compose.yml`.

## Permissions

The application runs as the unprivileged user `kiar`. Its UID/GID default to 1000 and can be set at
build time so they match the owner of the mapped host folders:

```bash
KIAR_UID=$(id -u) KIAR_GID=$(id -g) docker compose up -d --build
```

## Memory

`JAVA_OPTS` is passed straight to the JVM. The default `-XX:MaxRAMPercentage=75.0` sizes the heap
relative to the container's memory limit (set one via `deploy.resources.limits.memory`). Use explicit
values such as `-Xms512m -Xmx4g` if you prefer fixed sizes.

## Health check

The image declares a `HEALTHCHECK` that requests the dashboard index page every 30 seconds.
`docker ps` shows the container as `healthy` once the server accepts connections.

## Building the image manually

```bash
docker build -t kiar-ingest:1.5.0 .
docker run -d --name kiar-ingest -p 7070:7070 \
  -v /srv/kiar/config:/config -v /srv/kiar/db:/data/db -v /srv/kiar/ingest:/data/ingest \
  -v /srv/kiar/logs:/data/logs -v /srv/kiar/media:/data/media \
  kiar-ingest:1.5.0
```

The build stage uses the Gradle wrapper and downloads its own Node.js, so only Docker is required on
the build machine. Gradle's cache is kept in a BuildKit cache mount, which makes rebuilds after source
changes considerably faster.

## First administrator

A fresh database contains no users. Create the first administrator directly in the SQLite database
in the mapped `db` folder (the password is stored as a BCrypt hash), then log in and manage further
users through the dashboard.
