FROM maven:3.9.9-eclipse-temurin-21-jammy@sha256:7f8422bd81f19b9035007a75e2f82272b09af64db08848d7d4e93302466e8b6f as downloadBrowsers

COPY pom.xml createinstall.sh ./
RUN mkdir -p /opt/playwright/browsers && chmod -R 777 /opt/playwright
RUN ./createinstall.sh
RUN PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

FROM eclipse-temurin:21.0.7_6-jre-jammy@sha256:c4e6542e774de504da9b4729ff8d761287c965c1d788528ca78da30024efdb23

ARG USERNAME=playwright
ARG USER_UID=1000
ARG USER_GID=1000

LABEL org.opencontainers.image.source=https://github.com/acbgbca/pw-scraper
LABEL org.opencontainers.image.description="A service to scrap or screenshot dynamic websites using Microsoft Playwright."
LABEL org.opencontainers.image.licenses=AGPL3

RUN groupadd --gid $USER_GID $USERNAME && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME

RUN mkdir -p /opt/playwright/browsers && chown -R ${USERNAME}:${USERNAME} /opt/playwright && chmod -R 777 /opt/playwright

COPY --from=downloadBrowsers --chown=${USER_UID}:${USER_GID} --chmod=777 /opt/playwright /opt/playwright

RUN apt update && /opt/playwright/install.sh && apt clean && rm -rf /var/lib/apt/lists/*

USER ${USER_UID}:${USER_GID}

COPY target/quarkus-app /quarkus-app

HEALTHCHECK --interval=1m --timeout=3s \
    CMD curl -sf http://localhost:8080/healthcheck || exit 1

ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

EXPOSE 8080

ENTRYPOINT [ "java", "-XX:MinHeapFreeRatio=5", "-XX:MaxHeapFreeRatio=10", "-jar", "/quarkus-app/quarkus-run.jar" ]
