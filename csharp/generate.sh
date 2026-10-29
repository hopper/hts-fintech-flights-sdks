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

sdk_configs() {
  python3 - sdk/openapi-generator.json "$WORK_DIR" <<'PY'
import json, re, sys, pathlib
config_path, work = pathlib.Path(sys.argv[1]), pathlib.Path(sys.argv[2])
config = json.loads(config_path.read_text())
products = config.pop("sdkProducts")
product_files = config.pop("sdkProductFiles")

def accessor(names, tag):
    return names.get(tag) or "".join(w[:1].upper() + w[1:].lower() for w in re.findall(r"[A-Za-z0-9]+", tag))

for product in products:
    name = product["name"]
    spec = json.loads((work / product["spec"]).read_text())
    product["path"] = re.sub(r"^https?://[^/]+", "", spec["servers"][0]["url"])
    product["field"] = name[:1].lower() + name[1:]
    for ops in spec.get("paths", {}).values():
        for op in ops.values():
            if isinstance(op, dict) and op.get("tags"):
                op["x-sdk-accessor"] = accessor(product["accessors"], op["tags"][0])
    (work / f"sdk-{name}.spec.json").write_text(json.dumps(spec, indent=2) + "\n")
    files = {t: dict(f, destinationFilename=f["destinationFilename"].format(product=name)) for t, f in product_files.items()}
    (work / f"sdk-{name}.json").write_text(json.dumps(dict(config, sdkProduct=name, files=files), indent=2) + "\n")

shared = dict(config, sdkProducts=[{k: p[k] for k in ("name", "field", "path")} for p in products])
(work / "sdk-shared.json").write_text(json.dumps(shared, indent=2) + "\n")
print(" ".join(p["name"] for p in products))
PY
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

generate_sdk() {
  local spec="$1" config="$2" out="${WORK_DIR}/sdk"
  clean "$out"
  run_generator generate -i "$spec" -g csharp -o "$out" -t sdk/templates --config "$config" --skip-validate-spec
  python3 - "$config" "$out" "$PACKAGE_DIR" <<'PY'
import json, shutil, sys, pathlib
config, out, dest = (pathlib.Path(a) for a in sys.argv[1:4])
for spec in json.loads(config.read_text())["files"].values():
    target = dest / spec["destinationFilename"]
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(out / spec["destinationFilename"], target)
PY
}

PACKAGE_DIR="src/Hopper.Fintech.Flights"

clean "$PACKAGE_DIR"
generate_client "$(partner_api_only ../api/flights-cfar-v2.json)" cfar/openapi-generator.json "$PACKAGE_DIR/Cfar"
generate_client "$(partner_api_only ../api/flights-dafar-v2.json)" dg/openapi-generator.json "$PACKAGE_DIR/Dg"
generate_client ../api/spreedly.yaml spreedly/openapi-generator.json "$PACKAGE_DIR/Spreedly"

PRODUCTS="$(sdk_configs)"
for product in $PRODUCTS; do
  generate_sdk "${WORK_DIR}/sdk-${product}.spec.json" "${WORK_DIR}/sdk-${product}.json"
done
generate_sdk "${WORK_DIR}/sdk-${PRODUCTS%% *}.spec.json" "${WORK_DIR}/sdk-shared.json"
