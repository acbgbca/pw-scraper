FROM maven:3.9.7-amazoncorretto-21-debian as downloadBrowsers

COPY pom.xml ./pom.xml
RUN mkdir -p /opt/playwright/browsers && chmod -R 777 /opt/playwright
RUN PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"

FROM openjdk:21-slim-bullseye

ARG USERNAME=playwright
ARG USER_UID=1000
ARG USER_GID=1000

RUN groupadd --gid $USER_GID $USERNAME && useradd --uid $USER_UID --gid $USER_GID -m $USERNAME

RUN mkdir -p /opt/playwright/browsers && chown -R ${USERNAME}:${USERNAME} /opt/playwright && chmod -R 777 /opt/playwright

RUN apt-get update && export DEBIAN_FRONTEND=noninteractive && apt-get -y install --no-install-recommends curl libglib2.0-0\
 libnss3\
 libnspr4\
 libatk1.0-0\
 libatk-bridge2.0-0\
 libcups2\
 libdrm2\
 libdbus-1-3\
 libxcb1\
 libxkbcommon0\
 libatspi2.0-0\
 libx11-6\
 libxcomposite1\
 libxdamage1\
 libxext6\
 libxfixes3\
 libxrandr2\
 libgbm1\
 libpango-1.0-0\
 libcairo2\
 libasound2 && apt-get clean && rm -rf /var/lib/apt/lists/*

USER ${USER_UID}:${USER_GID}

COPY --from=downloadBrowsers --chown=${USER_UID}:${USER_GID} --chmod=777 /opt/playwright/browsers /opt/playwright/browsers

COPY target/quarkus-app /quarkus-app

HEALTHCHECK --interval=1m --timeout=3s \
    CMD curl -sf http://localhost:8080/healthcheck || exit 1

ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

EXPOSE 8080

ENTRYPOINT [ "java", "-XX:MinHeapFreeRatio=5", "-XX:MaxHeapFreeRatio=10", "-jar", "/quarkus-app/quarkus-run.jar" ]
