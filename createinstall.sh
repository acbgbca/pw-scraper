#!/bin/bash

echo '#!/bin/sh
' > /opt/playwright/install.sh
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install-deps --dry-run" | grep "^sh" >> /opt/playwright/install.sh
chmod a+rx /opt/playwright/install.sh