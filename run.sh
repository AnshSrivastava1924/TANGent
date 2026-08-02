#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f config/application.properties ]]; then
  echo "Missing config/application.properties. Copy .env.example values into a local Spring configuration first." >&2
  exit 1
fi

exec ./mvnw spring-boot:run
