#!/usr/bin/env bash
set -Eeuo pipefail

: "${POSTGRES_EXPORTER_PASSWORD}"

psql \
  --username "$POSTGRES_USER" \
  --dbname "$POSTGRES_DB" \
  --set=ON_ERROR_STOP=1 \
  --set=exporter_password="$POSTGRES_EXPORTER_PASSWORD" \
  --set=database_name="$POSTGRES_DB" <<'SQL'
CREATE USER postgres_exporter
    WITH PASSWORD :'exporter_password';

GRANT pg_monitor TO postgres_exporter;

GRANT CONNECT ON DATABASE :"database_name"
    TO postgres_exporter;
SQL