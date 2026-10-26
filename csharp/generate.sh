#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

GENERATOR_VERSION="${GENERATOR_VERSION:-7.26.0}"
WORK_DIR=".generator"
mkdir -p "$WORK_DIR"

run_generator() {
  if [ -z "${GENERATOR_NO_DOCKER:-}" ] && command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1; then
    docker run --rm -v "$(cd .. && pwd):/local" -w /local/csharp "openapitools/openapi-generator-cli:v${GENERATOR_VERSION}" "$@"
  else
    local jar="${WORK_DIR}/openapi-generator-cli-${GENERATOR_VERSION}.jar"
    if [ ! -f "$jar" ]; then
      curl -fsSL -o "$jar" "https://repo1.maven.org/maven2/org/openapitools/openapi-generator-cli/${GENERATOR_VERSION}/openapi-generator-cli-${GENERATOR_VERSION}.jar"
    fi
    java -jar "$jar" "$@"
  fi
}

partner_api_only() {
  local spec="$1" out="${WORK_DIR}/$(basename "$1")"
  python3 - "$spec" "$out" <<'PY'
import json, sys, pathlib
src, dst = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
spec = json.loads(src.read_text())
spec.pop("webhooks", None)
for path in [p for p, ops in spec.get("paths", {}).items()
             if any("Partner Endpoints" in op.get("tags", []) for op in ops.values() if isinstance(op, dict))]:
    del spec["paths"][path]
spec["tags"] = [t for t in spec.get("tags", []) if t.get("name") != "Partner Endpoints"]
dst.write_text(json.dumps(spec, indent=2) + "\n")
PY
  echo "$out"
}

with_sdk_accessors() {
  local spec="$1" out="${WORK_DIR}/sdk-$(basename "$1")"
  python3 - "$spec" "$out" sdk/openapi-generator.json <<'PY'
import json, re, sys, pathlib
src, dst, config = (pathlib.Path(a) for a in sys.argv[1:4])
spec = json.loads(src.read_text())
names = json.loads(config.read_text()).get("sdkAccessors", {})

def accessor(tag):
    return names.get(tag) or "".join(w[:1].upper() + w[1:].lower() for w in re.findall(r"[A-Za-z0-9]+", tag))

for ops in spec.get("paths", {}).values():
    for op in ops.values():
        if isinstance(op, dict) and op.get("tags"):
            op["x-sdk-accessor"] = accessor(op["tags"][0])
dst.write_text(json.dumps(spec, indent=2) + "\n")
PY
  echo "$out"
}

clean() {
  find "$1" -mindepth 1 -not -path "$1/bin*" -not -path "$1/obj*" -delete 2>/dev/null || true
}

generate_client() {
  local spec="$1" config="$2" dest="$3" out="${WORK_DIR}/$(basename "$(dirname "$2")")"
  local package
  package="$(python3 -c 'import json, sys; print(json.load(open(sys.argv[1]))["packageName"])' "$config")"
  clean "$out"
  run_generator generate -i "$spec" -g csharp -o "$out" --config "$config" --skip-validate-spec
  mkdir -p "$dest"
  for dir in Api Client Extensions Logging Model; do
    cp -R "$out/src/$package/$dir" "$dest/"
  done
  rm -f "$dest"/Extensions/IHost*Extensions.cs "$dest"/Extensions/IHttpClientBuilderExtensions.cs
}

SPEC="$(partner_api_only ../api/flights-cfar-v2.json)"
PACKAGE_DIR="src/Hopper.Fintech.Flights"

clean "$PACKAGE_DIR"
generate_client "$SPEC" cfar/openapi-generator.json "$PACKAGE_DIR/Cfar"
generate_client ../api/spreedly.yaml spreedly/openapi-generator.json "$PACKAGE_DIR/Spreedly"

SDK_OUT="${WORK_DIR}/sdk"
clean "$SDK_OUT"
run_generator generate \
  -i "$(with_sdk_accessors "$SPEC")" \
  -g csharp \
  -o "$SDK_OUT" \
  -t sdk/templates \
  --config sdk/openapi-generator.json \
  --skip-validate-spec

python3 - sdk/openapi-generator.json "$SDK_OUT" "$PACKAGE_DIR" <<'PY'
import json, shutil, sys, pathlib
config, out, dest = (pathlib.Path(a) for a in sys.argv[1:4])
for spec in json.loads(config.read_text())["files"].values():
    target = dest / spec["destinationFilename"]
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(out / spec["destinationFilename"], target)
PY
