#!/bin/sh
# Valencia one-shot release: bump -> assemble -> deploy -> README refs ->
# commit -> push -> GitHub release.
#
# Usage:  bash scripts/release.sh 1.7.52
#
# Prerequisites (script aborts if missing):
#   - .git/COMMIT_MSG.txt      commit message (write it first, -F style)
#   - .release_notes.tmp       release notes for gh --notes-file
#   - README.md already contains a "### vX.Y.Z" changelog entry
#
# Encodes the invariants: assemble only (never `build`), README version refs
# kept in sync, temp files cleaned up, gh auth via the token embedded in the
# origin remote URL.
set -e
cd "$(dirname "$0")/.."

VER="$1"
[ -n "$VER" ] || { echo "usage: bash scripts/release.sh X.Y.Z"; exit 1; }

# ── prerequisites ──────────────────────────────────────────────────────────
[ -f .git/COMMIT_MSG.txt ]  || { echo "ABORT: write .git/COMMIT_MSG.txt first"; exit 1; }
[ -f .release_notes.tmp ]   || { echo "ABORT: write .release_notes.tmp first"; exit 1; }
grep -q "### v$VER" README.md || { echo "ABORT: README.md has no '### v$VER' changelog entry"; exit 1; }

# sed, not grep -P — PCRE grep is locale-limited in this Git Bash.
OLD=$(sed -n 's/^mod_version=//p' gradle.properties | tr -d '\r')
[ "$OLD" != "$VER" ] || { echo "ABORT: version already $VER"; exit 1; }

# ── bump + build ───────────────────────────────────────────────────────────
sed -i "s/mod_version=$OLD/mod_version=$VER/" gradle.properties
./gradlew.bat assemble > /tmp/valencia_build.log 2>&1 \
  || { tail -20 /tmp/valencia_build.log; echo "ABORT: assemble failed (version reverted)"; \
       sed -i "s/mod_version=$VER/mod_version=$OLD/" gradle.properties; exit 1; }
echo "BUILD OK: valencia-$VER.jar"

# ── deploy ─────────────────────────────────────────────────────────────────
DEPLOY="$HOME/.lunarclient/profiles/1.21/mods/fabric-1.21.11"
cp "build/libs/valencia-$VER.jar" "$DEPLOY/"
rm -f "$DEPLOY/valencia-$OLD.jar" 2>/dev/null || echo "note: old jar locked (Lunar running?) — remove later"
echo "DEPLOYED to $DEPLOY"

# ── README version refs (changelog entry is the caller's job) ─────────────
sed -i "s/Latest: \*\*v$OLD\*\*/Latest: **v$VER**/; s|build/libs/valencia-$OLD\.jar|build/libs/valencia-$VER.jar|" README.md

# ── commit + push + release ────────────────────────────────────────────────
git add -A
git commit -F .git/COMMIT_MSG.txt
remote=$(git remote get-url origin)
GH_TOKEN=$(printf '%s' "$remote" | sed -n 's/.*:\(gh[ops]_[^@]*\)@.*/\1/p')
export GH_TOKEN
git push origin main
"/c/Users/Siesta/AppData/Local/gh-cli/bin/gh.exe" release create "v$VER" \
  "build/libs/valencia-$VER.jar" --title "v$VER" --notes-file .release_notes.tmp --latest

# ── cleanup ────────────────────────────────────────────────────────────────
rm -f .release_notes.tmp .git/COMMIT_MSG.txt
echo "RELEASED v$VER  https://github.com/Siesta0217/valencia/releases/tag/v$VER"
