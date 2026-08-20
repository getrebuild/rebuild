#!/bin/bash
# REBUILD one-click build (Linux/Unix)
# Usage:
#   ./build.sh
#   ./build.sh -mob
#   ./build.sh -mob -mobdir /path/to/rebuild-mob

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
NODE_VERSION="v20.18.0"
# NODE_DIST_URL="https://mirrors.tuna.tsinghua.edu.cn/nodejs-release"
NODE_DIST_URL="https://nodejs.org/dist"
NODE_DIR="$SCRIPT_DIR/.deploy/node"

ensure_node() {
  # 1) system node on PATH
  if command -v node >/dev/null 2>&1; then
    echo "Using system Node: $(node --version)"
    return
  fi
  # 2) previously downloaded portable node
  if [ -x "$NODE_DIR/bin/node" ]; then
    export PATH="$NODE_DIR/bin:$PATH"
    echo "Using portable Node: $(node --version)"
    return
  fi
  # 3) download portable node
  case "$(uname -m)" in
    x86_64|amd64) ARCH="linux-x64" ;;
    aarch64|arm64) ARCH="linux-arm64" ;;
    *) echo "Unsupported arch: $(uname -m)"; exit 1 ;;
  esac
  PKG="node-$NODE_VERSION-$ARCH"
  ARCHIVE="$PKG.tar.gz"
  URL="$NODE_DIST_URL/$NODE_VERSION/$ARCHIVE"
  TMP="/tmp/$ARCHIVE"
  TMPDIR="$NODE_DIR.tmp"
  echo "Node.js not found. Downloading portable $NODE_VERSION ($ARCH) ..."
  if command -v curl >/dev/null 2>&1; then
    curl -fSL -o "$TMP" "$URL"
  elif command -v wget >/dev/null 2>&1; then
    wget -q -O "$TMP" "$URL"
  else
    echo "Need curl or wget to download Node"; exit 1
  fi
  rm -rf "$TMPDIR"
  mkdir -p "$TMPDIR"
  tar -xzf "$TMP" -C "$TMPDIR"
  rm -f "$TMP"
  mkdir -p "$NODE_DIR"
  # move contents (including hidden files) out of the versioned dir
  mv "$TMPDIR/$PKG"/* "$NODE_DIR"/
  mv "$TMPDIR/$PKG"/.[!.]* "$NODE_DIR"/ 2>/dev/null || true
  rm -rf "$TMPDIR"
  export PATH="$NODE_DIR/bin:$PATH"
  echo "Installed portable Node: $(node --version)"
}

ensure_node

MOB=false
MOB_DIR="$(cd "$SCRIPT_DIR/.." && pwd)/rebuild-mob"

while [ $# -gt 0 ]; do
  case "$1" in
    -mob|--mob|-m) MOB=true; shift ;;
    -mobdir|--mob-dir) MOB_DIR="$2"; shift 2 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

if [ "$MOB" = "true" ]; then
  if [ -f "$MOB_DIR/package.json" ]; then
    (
      cd "$MOB_DIR"
      yarn install
      yarn build
    )
    DEST="$SCRIPT_DIR/src/main/resources/public/h5app"
    rm -rf "$DEST"
    mkdir -p "$DEST"
    cp -r "$MOB_DIR/build/." "$DEST"/
  else
    echo "rebuild-mob not found at '$MOB_DIR', skip building h5app. Use -mobdir to specify the path."
  fi
fi

chmod +x "$SCRIPT_DIR/mvnw"
cd "$SCRIPT_DIR"
./mvnw clean package -DskipTests

echo "Done: $SCRIPT_DIR/target/rebuild.jar"
