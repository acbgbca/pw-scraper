#!/bin/bash

echo '#!/bin/sh
' > /opt/playwright/install.sh

# Playwright's install-deps --dry-run requires up-to-date package lists to simulate apt installs
apt-get update -qq

# Playwright 1.52+ changed output from "sh -c apt-get..." lines to a plain package list.
# Extract the indented package names and build a single install command.
PACKAGES=$(mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps --dry-run" 2>&1 | \
    awk '/^Missing system dependencies/,0' | \
    grep -E '^  [a-z]' | \
    sed 's/^  //' | \
    tr '\n' ' ')

if [ -n "$PACKAGES" ]; then
    echo "apt-get install -y $PACKAGES" >> /opt/playwright/install.sh
fi

chmod a+rx /opt/playwright/install.sh
