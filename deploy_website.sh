#!/usr/bin/env bash

# Builds the documentation site.
#
#   ./deploy_website.sh --local   serve on localhost and reload on every edit
#   ./deploy_website.sh --ci      build into site/ and fail on any warning
#
# The served site lives under the same path as the published one, so open the
# address MkDocs prints rather than the bare host.
#
# The site needs Python. Install what it needs with:
#
#   python3 -m pip install -r mkdocs-requirements.txt
#
# Publishing happens from a GitHub Actions workflow, not from here.

set -euo pipefail

MODE="${1:---local}"

if [[ "${MODE}" != "--local" && "${MODE}" != "--ci" ]]; then
  echo "usage: $0 [--local|--ci]" >&2
  exit 1
fi

cd "$(dirname "$0")"

# The API reference is generated into docs/api and is not committed, so build it
# first. Without this the nav points at folders that do not exist yet.
./gradlew dokkaAll

# GitHub wants these two at the repository root, and the site wants them as
# pages. Copy rather than move, and keep the copies out of version control.
cp CHANGELOG.md docs/changelog.md
cp RELEASING.md docs/releasing.md

mkdir -p snippets
if [[ "${GITHUB_REF_TYPE:-}" == "tag" ]]; then
  VERSION="${GITHUB_REF_NAME#v}"
else
  VERSION="$(grep '^VERSION_NAME=' gradle/publishing.properties | cut -d= -f2)"
fi
printf 'app-gradle-plugins = "%s"' "${VERSION}" > snippets/catalog-version.md

if [[ "${MODE}" == "--local" ]]; then
  mkdocs serve
else
  mkdocs build --strict --site-dir site
fi
