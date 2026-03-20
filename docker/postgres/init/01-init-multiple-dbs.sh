#!/usr/bin/env bash
set -euo pipefail

create_role_if_missing() {
  local role_name="$1"
  local role_password="$2"

  if psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='${role_name}'" | grep -q 1; then
    echo "Role ${role_name} already exists"
  else
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE ROLE ${role_name} LOGIN PASSWORD '${role_password}';"
    echo "Created role ${role_name}"
  fi
}

create_db_if_missing() {
  local db_name="$1"
  local db_owner="$2"

  if psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${db_name}'" | grep -q 1; then
    echo "Database ${db_name} already exists"
  else
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "CREATE DATABASE ${db_name} OWNER ${db_owner};"
    echo "Created database ${db_name}"
  fi

  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres -c "GRANT ALL PRIVILEGES ON DATABASE ${db_name} TO ${db_owner};"
}

APP_DB_PASSWORD="${APP_DB_PASSWORD:-dev_password_123}"

echo "Initializing application databases and users"

create_role_if_missing "auth_user" "$APP_DB_PASSWORD"
create_role_if_missing "payment_user" "$APP_DB_PASSWORD"
create_role_if_missing "webhook_user" "$APP_DB_PASSWORD"

create_db_if_missing "auth_db" "auth_user"
create_db_if_missing "payment_db" "payment_user"
create_db_if_missing "webhook_db" "webhook_user"

echo "PostgreSQL initialization completed"

