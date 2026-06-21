FROM maven:3.9.16-eclipse-temurin-25-noble@sha256:01ef98a139ed64622c086bac54d1e167453d0f2ff68b69d00978f26d8736215c as downloadBrowsers

COPY pom.xml createinstall.sh ./
RUN mkdir -p /opt/playwright/browsers && chmod -R 777 /opt/playwright
RUN ./createinstall.sh
RUN PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

FROM eclipse-temurin:25.0.3_9-jre-noble@sha256:f9bd8815e73632c22985ebb133ec49b9fc4ad5ffe0657594ac02748ad0431ab7

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
