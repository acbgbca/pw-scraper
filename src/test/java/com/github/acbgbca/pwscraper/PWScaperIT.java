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
import org.testcontainers.containers.DockerComposeContainer;

class PWScaperIT {

  public static final String NGINX_IMAGE = "nginx:latest";

  private static RequestSpecification requestSpec;

  public static DockerComposeContainer environment;

  @BeforeAll
  static void setup() throws MalformedURLException {
    environment =
        new DockerComposeContainer(new File("docker-compose.yaml"))
            .withExposedService("pwscraper", 8080);
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
}
