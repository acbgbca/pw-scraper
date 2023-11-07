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

COPY target/playwright-proxy.jar /playwright-proxy.jar

HEALTHCHECK --interval=5m --timeout=3s \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright/browsers

EXPOSE 8080

ENTRYPOINT [ "java", "-Xms50M", "-jar", "/playwright-proxy.jar" ]
