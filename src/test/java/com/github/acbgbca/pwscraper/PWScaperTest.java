package com.github.acbgbca.pwscraper;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

@QuarkusTest
class PWScaperTest {

  public static final String NGINX_IMAGE = "nginx:latest";

  private static NginxContainer<?> nginx;
  private static URL baseUrl;

  @BeforeAll
  static void setup() throws MalformedURLException {
    nginx =
        new NginxContainer<>(NGINX_IMAGE)
            .withFileSystemBind(
                "src/test/resources/nginx/www", "/usr/share/nginx/html", BindMode.READ_ONLY)
            .waitingFor(new HttpWaitStrategy());
    nginx.start();
    baseUrl = nginx.getBaseUrl("http", 80);
  }

  @AfterAll
  static void cleanUp() {
    nginx.close();
  }

  @Test
  void testGetContentHtml() {
    given()
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url={url}", baseUrl.toString())
        .then()
        .statusCode(200)
        .body(containsString("<p>stuff</p>"));
  }

  @Test
  void testGetContentJavascript() {
    given()
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url={url}/javascript.html", baseUrl.toString())
        .then()
        .statusCode(200)
        .body(containsString("""
          <p id="change">Hello JavaScript!</p>
          """));
  }

  @Test
  void testGetContentReact() {
    given()
        .config(
            RestAssuredConfig.config()
                .httpClient(
                    HttpClientConfig.httpClientConfig().setParam("http.connection.timeout", 60000)))
        .when()
        .get("/content?url={url}/react.html", baseUrl.toString())
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
