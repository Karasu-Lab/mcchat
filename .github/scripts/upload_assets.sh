#!/usr/bin/env bash
set -euo pipefail

tag="$1"
version="$2"
shift 2

table="| File | Version | SHA256 |"$'\n'"| --- | --- | --- |"
for file in "$@"; do
  hash=$(sha256sum "$file" | cut -d' ' -f1)
  table+=$'\n'"| \`$(basename "$file")\` | $version | \`$hash\` |"
done

body=$(gh release view "$tag" --json body --jq .body)
gh release edit "$tag" --notes "$body"$'\n\n'"$table"
gh release upload "$tag" "$@" --clobber
