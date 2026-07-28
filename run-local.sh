#!/usr/bin/env bash
# Uruchamia backend lokalnie podpiety pod Neon (Postgres).
# Wczytuje zmienne z .env.local (plik jest w .gitignore - haslo nie trafia do repo).
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env.local ]; then
  echo "Brak .env.local. Zrob: cp .env.local.example .env.local i wpisz DB_PASS z Neon." >&2
  exit 1
fi

# Wczytaj zmienne z .env.local do srodowiska.
set -a
# shellcheck disable=SC1091
. ./.env.local
set +a

if [ "${DB_PASS:-}" = "WKLEJ_TUTAJ_HASLO_Z_NEON" ] || [ -z "${DB_PASS:-}" ]; then
  echo "Ustaw prawdziwe DB_PASS w .env.local (teraz jest placeholder)." >&2
  exit 1
fi

exec ./mvnw spring-boot:run
