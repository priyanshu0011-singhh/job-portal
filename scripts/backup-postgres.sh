#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
env_file="${JOBPORTAL_ENV_FILE:-${project_dir}/.env.production}"
backup_dir="${BACKUP_DIR:-${project_dir}/backups}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_file="${backup_dir}/jobportal-${timestamp}.dump"

if [[ ! -f "${env_file}" ]]; then
    echo "Missing ${env_file}; set JOBPORTAL_ENV_FILE to the Compose env file." >&2
    exit 1
fi

mkdir -p "${backup_dir}"
docker compose --env-file "${env_file}" exec -T db sh -c \
    'pg_dump --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --format=custom' \
    > "${backup_file}"

if [[ ! -s "${backup_file}" ]]; then
    rm -f "${backup_file}"
    echo "Database backup is empty." >&2
    exit 1
fi

echo "Created ${backup_file}; copy it to encrypted off-host storage and test restores."