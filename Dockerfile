FROM maven:3.9.9-eclipse-temurin-21-jammy@sha256:3df7f4a433809a4362fe8a91e3f31521146309cb17cf2e141b3f847aa6c2ded1 as downloadBrowsers

COPY pom.xml createinstall.sh ./
RUN mkdir -p /opt/playwright/browsers && chmod -R 777 /opt/playwright
RUN ./createinstall.sh
RUN PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

FROM eclipse-temurin:21.0.5_11-jre-jammy@sha256:5f8358c9d5615c18e95728e8b8528bda7ff40a7a5da2ac9a35b7a01f5d9b231a

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
