#!/bin/bash
set -euo pipefail

echo '#!/bin/sh
' > /opt/playwright/install.sh

# Playwright's install-deps --dry-run requires up-to-date package lists to simulate apt installs
apt-get update -qq

# The Playwright driver is a Node binary that needs libatomic to run at all. install-deps can only report what the
# browsers need, and only once the driver runs, so install it here and add it to the generated script explicitly
# (install-deps would otherwise treat it as already satisfied and leave it out).
DRIVER_PACKAGES="libatomic1"
apt-get install -y --no-install-recommends $DRIVER_PACKAGES

# The dry-run exits non-zero when dependencies are missing, so a failure is only an error if it also produced no package list
# (e.g. the Playwright driver could not start). Fail loudly rather than silently generating an empty install script.
STATUS=0
DRY_RUN=$(mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps --dry-run" 2>&1) || STATUS=$?
if [ "$STATUS" -ne 0 ] && ! grep -q '^Missing system dependencies' <<< "$DRY_RUN"; then
    echo "$DRY_RUN" >&2
    echo "ERROR: playwright install-deps --dry-run failed (exit $STATUS)" >&2
    exit 1
fi

# Playwright 1.52+ changed output from "sh -c apt-get..." lines to a plain package list.
# Extract the indented package names and build a single install command.
PACKAGES=$(echo "$DRY_RUN" | \
    awk '/^Missing system dependencies/,0' | \
    { grep -E '^  [a-z]' || true; } | \
    sed 's/^  //' | \
    tr '\n' ' ')

echo "apt-get install -y $DRIVER_PACKAGES $PACKAGES" >> /opt/playwright/install.sh

chmod a+rx /opt/playwright/install.sh
