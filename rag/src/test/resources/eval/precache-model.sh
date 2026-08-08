#!/usr/bin/env bash
# Downloads the ONNX embedding model to a local directory, so the app can load it from a
# file: URI instead of a remote URL.
#
# WHY: Spring AI's ResourceCacheService reads a *remote* resource entirely into a byte[]
# before writing it to disk (StreamUtils.copyToByteArray -> InputStream.readAllBytes). For the
# 440MB BGE-base model that needs ~440MB of contiguous heap and dies with OutOfMemoryError on
# a default-sized JVM. curl streams to disk, and a local file: URI bypasses that caching path
# entirely - so this fixes the cause rather than accommodating it with a bigger heap.
#
# After running this, point application.yml at the local files:
#   EMBEDDING_MODEL_URI=file:<repo>/.models/bge-base-en-v1.5/model.onnx
#   EMBEDDING_TOKENIZER_URI=file:<repo>/.models/bge-base-en-v1.5/tokenizer.json
#
# Usage: bash rag/src/test/resources/eval/precache-model.sh
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../../../.." && pwd)"
DEST="${MODEL_DIR:-${REPO_ROOT}/.models/bge-base-en-v1.5}"

MODEL_URL="${EMBEDDING_MODEL_URL:-https://huggingface.co/BAAI/bge-base-en-v1.5/resolve/main/onnx/model.onnx}"
TOKENIZER_URL="${EMBEDDING_TOKENIZER_URL:-https://huggingface.co/BAAI/bge-base-en-v1.5/resolve/main/tokenizer.json}"

mkdir -p "$DEST"

fetch() {
  local url="$1" out="$2"
  if [[ -s "$out" ]]; then
    echo "  already present: $out ($(du -h "$out" | cut -f1))"
    return
  fi
  echo "  downloading $(basename "$out") ..."
  # -L follows HuggingFace's CDN redirect. Streams to disk, never through the heap.
  curl -sSL --fail --max-time 900 -o "${out}.part" "$url"
  mv "${out}.part" "$out"
  echo "  done: $out ($(du -h "$out" | cut -f1))"
}

echo "model dir: $DEST"
fetch "$TOKENIZER_URL" "${DEST}/tokenizer.json"
fetch "$MODEL_URL"     "${DEST}/model.onnx"

cat <<EOF

Pre-cache complete. Local URIs for application.yml / .env:

  EMBEDDING_MODEL_URI=file:${DEST}/model.onnx
  EMBEDDING_TOKENIZER_URI=file:${DEST}/tokenizer.json
EOF
