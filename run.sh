#!/usr/bin/env bash
# Build and run the whole BidAndDrive stack (PostgreSQL, Keycloak, backend, frontend) with Docker compose.
#
#   ./run.sh            build the images, start everything, wait until it answers, print the URLs
#   ./run.sh --test     same, but run the backend and frontend test suites first (needs Java 25, Maven, Node)
#   ./run.sh status     show the containers
#   ./run.sh logs       follow the logs (Ctrl+C to quit)
#   ./run.sh stop       stop and remove the containers, keep the database
#   ./run.sh clean      stop and remove the containers AND the data
#
# Ports can be changed with POSTGRES_PORT, KEYCLOAK_PORT and BACKEND_PORT (the frontend must stay on 5173).
set -euo pipefail

cd "$(dirname "$0")"

POSTGRES_PORT="${POSTGRES_PORT:-5433}"
KEYCLOAK_PORT="${KEYCLOAK_PORT:-8180}"
BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT=5173
WAIT_SECONDS="${WAIT_SECONDS:-180}"

if [[ -t 1 ]]; then
  BOLD=$'\e[1m' GREEN=$'\e[32m' RED=$'\e[31m' YELLOW=$'\e[33m' RESET=$'\e[0m'
else
  BOLD='' GREEN='' RED='' YELLOW='' RESET=''
fi

info() { printf '%s==>%s %s\n' "$BOLD" "$RESET" "$*"; }
ok() { printf '%s ✓%s %s\n' "$GREEN" "$RESET" "$*"; }
warn() { printf '%s !%s %s\n' "$YELLOW" "$RESET" "$*"; }
fail() {
  printf '%s ✗ %s%s\n' "$RED" "$*" "$RESET" >&2
  exit 1
}

usage() { sed -n '2,12s/^# \{0,1\}//p' "$0"; }

compose() { docker compose "$@"; }

check_prerequisites() {
  command -v docker >/dev/null 2>&1 || fail "Docker is not installed (https://docs.docker.com/get-docker/)."
  docker info >/dev/null 2>&1 || fail "Docker is installed but not running, or this user cannot use it."
  docker compose version >/dev/null 2>&1 || fail "The Docker compose plugin is missing ('docker compose')."
  command -v curl >/dev/null 2>&1 || fail "curl is required to check that the services are up."
}

port_in_use() {
  (exec 3<>"/dev/tcp/127.0.0.1/$1") >/dev/null 2>&1
}

check_ports() {
  if [[ -n "$(compose ps -q 2>/dev/null)" ]]; then
    return # our own stack already holds the ports; compose will update it in place
  fi
  local busy=()
  local name port
  for name in "PostgreSQL:$POSTGRES_PORT" "Keycloak:$KEYCLOAK_PORT" "Backend:$BACKEND_PORT" "Frontend:$FRONTEND_PORT"; do
    port="${name#*:}"
    if port_in_use "$port"; then
      busy+=("${name%%:*} port $port")
    fi
  done
  if ((${#busy[@]})); then
    fail "Port(s) already in use: ${busy[*]}. Stop what uses them, or set POSTGRES_PORT / KEYCLOAK_PORT / BACKEND_PORT."
  fi
}

run_tests() {
  info "Running the backend tests (mvn verify, needs Docker for Testcontainers)"
  command -v mvn >/dev/null 2>&1 || fail "Maven is required for --test."
  (cd backend && mvn -B -q verify) || fail "Backend tests failed."
  ok "Backend tests passed"

  info "Running the frontend tests and type-check"
  command -v npm >/dev/null 2>&1 || fail "Node.js / npm is required for --test."
  (cd frontend && npm ci --no-audit --no-fund --silent && npm test --silent && npm run build --silent) \
    || fail "Frontend tests or build failed."
  ok "Frontend tests and build passed"
}

wait_for() {
  local label="$1" url="$2" service="$3"
  local deadline=$((SECONDS + WAIT_SECONDS))
  printf '   waiting for %s ' "$label"
  until curl -sf -o /dev/null "$url"; do
    if ((SECONDS >= deadline)); then
      printf '\n'
      warn "Last log lines of '$service':"
      compose logs --tail 30 "$service" >&2 || true
      fail "$label did not answer at $url within ${WAIT_SECONDS}s."
    fi
    printf '.'
    sleep 2
  done
  printf '\n'
  ok "$label is up"
}

start() {
  check_prerequisites
  check_ports
  if [[ "${1:-}" == "--test" ]]; then
    run_tests
  fi

  info "Building the images (the first build downloads dependencies and takes a few minutes)"
  compose build
  info "Starting PostgreSQL, Keycloak, backend and frontend"
  compose up -d

  wait_for "Keycloak" "http://localhost:$KEYCLOAK_PORT/realms/bidanddrive/.well-known/openid-configuration" keycloak
  wait_for "Backend API" "http://localhost:$BACKEND_PORT/api/auctions/running" backend
  wait_for "Frontend" "http://localhost:$FRONTEND_PORT/api/auctions/running" frontend

  cat <<EOF

${GREEN}${BOLD}BidAndDrive is running.${RESET}

  App              http://localhost:$FRONTEND_PORT        log in as seller/seller or buyer/buyer
  API (Swagger)    http://localhost:$BACKEND_PORT/swagger-ui/index.html
  Keycloak admin   http://localhost:$KEYCLOAK_PORT        admin/admin
  PostgreSQL       localhost:$POSTGRES_PORT  db auction   auction/auction

  ./run.sh logs    follow the logs
  ./run.sh stop    stop (keeps data)      ./run.sh clean    stop and delete data
EOF
}

case "${1:-}" in
  "" | --test) start "${1:-}" ;;
  status) compose ps ;;
  logs) compose logs -f ;;
  stop)
    compose down
    ok "Stopped (data kept)."
    ;;
  clean)
    compose down -v
    ok "Stopped and data deleted."
    ;;
  -h | --help | help) usage ;;
  *)
    usage >&2
    exit 1
    ;;
esac
