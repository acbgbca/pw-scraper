FROM maven:3.9.16-eclipse-temurin-25-noble@sha256:7e461cec477077c1d9e50b13df8aef9018764410f4c4cd7c34803f10c4c99e4c as downloadBrowsers

COPY pom.xml createinstall.sh ./
RUN mkdir -p /opt/playwright/browsers && chmod -R 777 /opt/playwright
RUN ./createinstall.sh
RUN PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:2f1da100788559b397bcf48c736169ea5b070bde84e55f203bbee8e83d87a175

ARG USERNAME=playwright
ARG USER_UID=999
ARG USER_GID=999

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
