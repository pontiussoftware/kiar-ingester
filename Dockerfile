# syntax=docker/dockerfile:1
#
# KIAR Ingest -- multi-stage build.
#
#   Stage 1 (build):   compiles the Kotlin backend and the Angular frontend with the Gradle wrapper.
#                      The node-gradle plugin downloads its own Node.js, so only a JDK is needed here.
#   Stage 2 (runtime): minimal JRE image running the installed distribution as an unprivileged user.
#
# All state lives below /data and /config so that host folders can be mapped in (see docker-compose.yml
# and doc/DOCKER.md). Image deployment targets and image source folders configured in the UI must point
# to paths *inside* the container, i.e. to one of the mapped folders.

# ---------------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk AS build

# The Node.js binary downloaded by the node-gradle plugin needs libatomic at runtime.
RUN apt-get update \
 && apt-get install -y --no-install-recommends libatomic1 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /src

# Build scripts first so that dependency resolution can be cached independently of source changes.
COPY gradlew build.gradle.kts settings.gradle.kts gradle.properties openapitools.json ./
COPY gradle ./gradle
# The wrapper script may carry Windows line endings when checked out on Windows.
RUN sed -i 's/\r$//' ./gradlew && chmod +x ./gradlew

COPY doc ./doc
COPY kiar-ui ./kiar-ui
COPY kiar-ingest ./kiar-ingest

# Gradle home is cached between builds (dependencies, wrapper distribution, downloaded Node.js).
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :kiar-ingest:installDist --no-daemon --console=plain

# ---------------------------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime

# UID/GID of the user running the application. Match them to the owner of the mapped host folders,
# e.g. `docker build --build-arg KIAR_UID=$(id -u) --build-arg KIAR_GID=$(id -g) .`
ARG KIAR_UID=1000
ARG KIAR_GID=1000

# curl is only used by the HEALTHCHECK below. The Ubuntu base image ships a stock "ubuntu" user with
# UID/GID 1000; it is removed so that the requested IDs are free for the "kiar" user.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && (id ubuntu > /dev/null 2>&1 && userdel --remove ubuntu || true) \
 && (getent group ubuntu > /dev/null && groupdel ubuntu || true) \
 && groupadd --gid ${KIAR_GID} kiar \
 && useradd --uid ${KIAR_UID} --gid kiar --create-home --shell /usr/sbin/nologin kiar \
 && mkdir -p /config /data/db /data/ingest /data/logs /data/media \
 && chown -R kiar:kiar /config /data

COPY --from=build /src/kiar-ingest/build/install/kiar-ingest /opt/kiar
COPY docker/entrypoint.sh /opt/kiar/entrypoint.sh
RUN sed -i 's/\r$//' /opt/kiar/entrypoint.sh && chmod +x /opt/kiar/entrypoint.sh

# Runtime configuration. KIAR_CONFIG is read if it exists; otherwise the entrypoint generates it from
# the KIAR_* variables below.
ENV KIAR_CONFIG=/config/config.json \
    KIAR_DB_PATH=/data/db/kiar.db \
    KIAR_INGEST_PATH=/data/ingest \
    KIAR_LOG_PATH=/data/logs \
    KIAR_WEB_PORT=7070 \
    KIAR_JOB_LOG_RETENTION_DAYS=30 \
    KIAR_INPUT_RETENTION_COUNT=1 \
    KIAR_SECURE_COOKIES=false \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

USER kiar
WORKDIR /opt/kiar

# /config          configuration file (config.json)
# /data/db         SQLite database
# /data/ingest     uploaded KIAR files and watched job template sources, one sub-folder per participant
# /data/logs       application log (kiar.log)
# /data/media      default target for image deployments (configured per Apache Solr deployment in the UI)
VOLUME ["/config", "/data/db", "/data/ingest", "/data/logs", "/data/media"]

EXPOSE 7070

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
    CMD curl -fsS "http://localhost:${KIAR_WEB_PORT}/" > /dev/null || exit 1

ENTRYPOINT ["/opt/kiar/entrypoint.sh"]
