#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(dirname "$(readlink -f "${BASH_SOURCE[0]}")")"
REPO_ROOT="$(git -C "$SCRIPT_DIR" rev-parse --show-toplevel 2>/dev/null)" || exit 0
cd "$REPO_ROOT"

OUTPUT_DIR="docs/llm"
OUTPUT_FILE="$OUTPUT_DIR/dump.txt"
SKIP='^(docs/llm/|gradlew$|gradlew\.bat$|gradle/wrapper/|screenshots/|LICENSE$|CODE_OF_CONDUCT\.md$|app/src/[^/]+/res/(drawable[^/]*|mipmap[^/]*|font|raw)/|app/src/main/res/values-[^/]+/strings\.xml$|app/src/main/assets/(html|css|fonts|svg)/|app/databases/schemas/)'

mapfile -t FILES < <(git ls-files --cached --others --exclude-standard | grep -Ev '^docs/llm/' | LC_ALL=C sort -u)

mkdir -p "$OUTPUT_DIR"
TMP_FILE="$(mktemp "$OUTPUT_DIR/.dump.XXXXXX")"
trap 'rm -f "$TMP_FILE"' EXIT

{
    echo "# REPOSITORY CONTEXT DUMP"
    echo "Generated: $(date --iso-8601=seconds)"
    echo "Branch:    $(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo none)"
    echo "Commit:    $(git log -1 --pretty='%h %ci %s' 2>/dev/null || echo none)"
    echo "Remote:    $(git remote get-url origin 2>/dev/null || echo none)"
    echo
    echo "## STATUS"
    git status --short | grep -v ' docs/llm/' || true
    echo
    echo "## TREE (${#FILES[@]} files)"
    printf '%s\n' "${FILES[@]}" | awk -F/ '{
        same = 1
        for (i = 1; i < NF; i++) {
            if (same && prev[i] == $i) continue
            same = 0
            printf "%*s%s/\n", (i - 1) * 2, "", $i
        }
        printf "%*s%s\n", (NF - 1) * 2, "", $NF
        for (i = 1; i < NF; i++) prev[i] = $i
        for (i = NF; i in prev; i++) delete prev[i]
    }'
    for f in "${FILES[@]}"; do
        [[ -f "$f" ]] || continue
        [[ "$f" =~ $SKIP ]] && continue
        [[ ! -s "$f" ]] || grep -Iq . "$f" || continue
        echo
        echo "################################################################################"
        echo "# FILE: $f"
        echo "################################################################################"
        cat "$f"
        [[ -z "$(tail -c1 "$f")" ]] || echo
    done
} > "$TMP_FILE"

mv -f "$TMP_FILE" "$OUTPUT_FILE"
trap - EXIT
echo "$OUTPUT_FILE: $(wc -c < "$OUTPUT_FILE") bytes"
