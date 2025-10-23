package com.github.acbgbca.pwscraper;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.restassured.builder.RequestSpecBuilder;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.specification.RequestSpecification;
import java.io.File;
import java.net.MalformedURLException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

class PWScaperIT {
  private static Logger logger = LoggerFactory.getLogger(PWScaperIT.class);

  public static final String NGINX_IMAGE = "nginx:latest";

  private static RequestSpecification requestSpec;

  public static ComposeContainer environment;

  @BeforeAll
  static void setup() throws MalformedURLException {
    environment =
        new ComposeContainer(new File("docker-compose.yaml")).withExposedService("pwscraper", 8080);
    environment.withLogConsumer("pwscrapper", new Slf4jLogConsumer(logger));
    environment.start();
    String baseUrl =
        "http://"
            + environment.getServiceHost("pwscraper", 8080)
            + ":"
            + environment.getServicePort("pwscraper", 8080);
    requestSpec = new RequestSpecBuilder().setBaseUri(baseUrl).build();
  }

  @AfterAll
  static void cleanUp() {
    environment.close();
  }

  @Test
  void testGetContentHtml() {
    given(requestSpec)
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url=http://nginx/index.html")
        .then()
        .statusCode(200)
        .body(containsString("<p>stuff</p>"));
  }

  @ParameterizedTest(name = "get HTML content with browser {0}")
  @EnumSource(Engine.class)
  void testGetContentHtmlBrowserOverride(Engine browser) {
    given(requestSpec)
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url={url}&browser={browser}", "http://nginx/index.html", browser.name())
        .then()
        .statusCode(200)
        .body(containsString("<p>stuff</p>"));
  }

  @Test
  void testGetContentJavascript() {
    given(requestSpec)
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url=http://nginx/javascript.html")
        .then()
        .statusCode(200)
        .body(containsString("""
          <p id="change">Hello JavaScript!</p>
          """));
  }

  @ParameterizedTest(name = "get Javascript content with browser {0}")
  @EnumSource(Engine.class)
  void testGetContentJavascriptBrowserOverride(Engine browser) {
    given(requestSpec)
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url={url}&browser={browser}", "http://nginx/javascript.html", browser.name())
        .then()
        .statusCode(200)
        .body(containsString("""
          <p id="change">Hello JavaScript!</p>
          """));
  }

  @Test
  void testGetContentReact() {
    given(requestSpec)
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url=http://nginx/react.html")
        .then()
        .statusCode(200)
        .body(
            containsString(
                "<img src=\"/static/media/logo.6ce24c58023cc2f8fd88fe9d219db6c6.svg\" class=\"App-logo\" alt=\"logo\">"),
            containsString("<p>Edit <code>src/App.js</code> and save to reload.</p>"),
            containsString(
                "<a class=\"App-link\" href=\"https://reactjs.org\" target=\"_blank\" rel=\"noopener noreferrer\">Learn React</a>"));
  }

  @ParameterizedTest(name = "get React content with browser {0}")
  @EnumSource(Engine.class)
  void testGetContentReactBrowserOverride(Engine browser) {
    given(requestSpec)
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url={url}&browser={browser}", "http://nginx/react.html", browser.name())
        .then()
        .statusCode(200)
        .body(
            containsString(
                "<img src=\"/static/media/logo.6ce24c58023cc2f8fd88fe9d219db6c6.svg\" class=\"App-logo\" alt=\"logo\">"),
            containsString("<p>Edit <code>src/App.js</code> and save to reload.</p>"),
            containsString(
                "<a class=\"App-link\" href=\"https://reactjs.org\" target=\"_blank\" rel=\"noopener noreferrer\">Learn React</a>"));
  }
}
