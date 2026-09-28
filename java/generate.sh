#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

GENERATOR_VERSION="${GENERATOR_VERSION:-7.10.0}"
WORK_DIR=".gradle/generator"
mkdir -p "$WORK_DIR"

run_generator() {
  if [ -z "${GENERATOR_NO_DOCKER:-}" ] && command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    docker run --rm -v "$(cd .. && pwd):/local" -w /local/java "openapitools/openapi-generator-cli:v${GENERATOR_VERSION}" "$@"
  else
    local jar="${WORK_DIR}/openapi-generator-cli-${GENERATOR_VERSION}.jar"
    if [ ! -f "$jar" ]; then
      curl -fsSL -o "$jar" "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${GENERATOR_VERSION}/openapi-generator-cli-${GENERATOR_VERSION}.jar"
    fi
    java -jar "$jar" "$@"
  fi
}

without_webhooks() {
  local spec="$1" out="${WORK_DIR}/$(basename "$1")"
  python3 - "$spec" "$out" <<'EOF'
import json, sys, pathlib
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
if src.suffix == ".json":
    spec = json.loads(src.read_text())
    spec.pop("webhooks", None)
    dst.write_text(json.dumps(spec, indent=2) + "\n")
else:
    dst.write_text(src.read_text())
EOF
  echo "$out"
}

generate() {
  local spec="$1" module="$2"
  run_generator generate \
    -i "$(without_webhooks "$spec")" \
    -g java \
    -o "$module" \
    --config "${module}/openapi-generator.json" \
    --skip-validate-spec
}

generate ../api/flights-cfar-v2.json flights-cfar-client
generate ../api/spreedly.yaml spreedly-client
