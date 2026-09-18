#!/usr/bin/env bash
#
# Entrypoint for the KIAR Ingest container.
#
#  * Uses the configuration file at $KIAR_CONFIG if one is mapped in.
#  * Otherwise generates it from the KIAR_* environment variables (see Dockerfile / docker-compose.yml).
#  * Makes sure the folders referenced by the configuration exist, then starts the server.
#
# The JVM is started directly (not via the Gradle start script) so that JAVA_OPTS fully controls the
# memory settings; the default -XX:MaxRAMPercentage=75.0 respects the container's memory limit.
set -euo pipefail

KIAR_CONFIG="${KIAR_CONFIG:-/config/config.json}"

if [ ! -f "${KIAR_CONFIG}" ]; then
  echo "No configuration found at ${KIAR_CONFIG}; generating one from environment variables."
  mkdir -p "$(dirname "${KIAR_CONFIG}")"
  cat > "${KIAR_CONFIG}" <<EOF
{
  "web": true,
  "webPort": ${KIAR_WEB_PORT:-7070},
  "dbPath": "${KIAR_DB_PATH:-/data/db/kiar.db}",
  "ingestPath": "${KIAR_INGEST_PATH:-/data/ingest}",
  "logPath": "${KIAR_LOG_PATH:-/data/logs}",
  "jobLogRetentionDays": ${KIAR_JOB_LOG_RETENTION_DAYS:-30},
  "inputRetentionCount": ${KIAR_INPUT_RETENTION_COUNT:-1}
}
EOF
fi

# Folders referenced by the configuration must exist before the server starts (SQLite creates the
# database file, but not its parent directory; Log4j needs the log folder).
read_path() { sed -n "s/^[[:space:]]*\"$1\"[[:space:]]*:[[:space:]]*\"\([^\"]*\)\".*/\1/p" "${KIAR_CONFIG}" | head -n 1; }
DB_PATH="$(read_path dbPath)"
INGEST_PATH="$(read_path ingestPath)"
LOG_PATH="$(read_path logPath)"
[ -n "${DB_PATH}" ] && mkdir -p "$(dirname "${DB_PATH}")"
[ -n "${INGEST_PATH}" ] && mkdir -p "${INGEST_PATH}"
[ -n "${LOG_PATH}" ] && mkdir -p "${LOG_PATH}"

echo "Starting KIAR Ingest with configuration ${KIAR_CONFIG} (JAVA_OPTS: ${JAVA_OPTS:-<none>})"
# shellcheck disable=SC2086
exec java ${JAVA_OPTS:-} -cp "/opt/kiar/lib/*" ch.pontius.kiar.ApplicationKt "${KIAR_CONFIG}" "$@"
