package com.github.acbgbca.pwscraper;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.ScreenshotOptions;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/content")
public class PWScaper {

  private Logger log = LoggerFactory.getLogger(PWScaper.class);

  @PostConstruct
  public void init() {
    try (Playwright unused = Playwright.create()) {
      log.info("Browsers downloaded");
    }
  }

  @GET
  public jakarta.ws.rs.core.Response getHtml(@QueryParam("url") String url) {

    Long startTime = System.currentTimeMillis();
    log.info("Retrieving content from location: {}", url);
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage(); ) {
      // Don't download images
      page.route(
          Pattern.compile(".*\\.(jpg|gif|png)"),
          route -> {
            log.debug("Aborting {}", route.toString());
            route.abort();
          });
      // Load page and wait for content to load
      Response pageResponse = page.navigate(url);
      page.waitForLoadState(LoadState.NETWORKIDLE);

      // Gradually scroll the page down one screen at a time, waiting for the content to load
      for (int i = 0; i < 25; i++) {
        page.mouse().wheel(0, page.viewportSize().height);
        page.waitForLoadState(LoadState.NETWORKIDLE);
      }

      // Add a base tag with the correct base URL
      // Fixes display in browsers, won't help with scraping tools
      page.evaluate(
          String.format(
              """
                const element = document.createElement('base');%n\
                element.href = '%s';%n\
                document.head.insertBefore(element, document.head.firstChild);%n\
                """,
              url));

      ResponseBuilder response =
          jakarta.ws.rs.core.Response.ok(page.content(), pageResponse.headerValue("Content-Type"));
      log.info("Retrieved content ins {} milliseconds", System.currentTimeMillis() - startTime);
      return response.build();
    } finally {
      // Force run a FULL GC cycle.
      // Without this Java will run a partial GC which doesn't reclaim as much memory.
      // Drops the post retrieve memory usage by about 20%
      System.gc();
    }
  }

  @GET
  @Path("/image")
  public jakarta.ws.rs.core.Response getImage(@QueryParam("url") String url) {

    Long startTime = System.currentTimeMillis();
    log.info("Retrieving image from location: {}", url);
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage(); ) {
      // Load page and wait for content to load
      page.navigate(url);
      page.waitForLoadState(LoadState.NETWORKIDLE);

      // // Gradually scroll the page down one screen at a time, waiting for the content to load
      // for (int i = 0; i < 25; i++) {
      //     page.mouse().wheel(0, page.viewportSize().height);
      //     page.waitForLoadState(LoadState.NETWORKIDLE);
      // }
      ScreenshotOptions options = new ScreenshotOptions();
      options.setFullPage(true);

      ResponseBuilder response =
          jakarta.ws.rs.core.Response.ok(page.screenshot(options), "image/png");
      log.info("Retrieved image in {} milliseconds", System.currentTimeMillis() - startTime);
      return response.build();
    } finally {
      // Force run a FULL GC cycle.
      // Without this Java will run a partial GC which doesn't reclaim as much memory.
      // Drops the post retrieve memory usage by about 20%
      System.gc();
    }
  }

  @GET
  @Path("/pdf")
  public jakarta.ws.rs.core.Response getPdf(@QueryParam("url") String url) {

    Long startTime = System.currentTimeMillis();
    log.info("Retrieving pdf from location: {}", url);
    try (Playwright playwright = Playwright.create();
        Browser browser = playwright.chromium().launch();
        Page page = browser.newPage(); ) {
      // Load page and wait for content to load
      page.navigate(url);
      page.waitForLoadState(LoadState.NETWORKIDLE);

      // // Gradually scroll the page down one screen at a time, waiting for the content to load
      // for (int i = 0; i < 25; i++) {
      //     page.mouse().wheel(0, page.viewportSize().height);
      //     page.waitForLoadState(LoadState.NETWORKIDLE);
      // }

      ResponseBuilder response = jakarta.ws.rs.core.Response.ok(page.pdf(), "application/pdf");
      log.info("Retrieved pdf in {} milliseconds", System.currentTimeMillis() - startTime);
      return response.build();
    } finally {
      // Force run a FULL GC cycle.
      // Without this Java will run a partial GC which doesn't reclaim as much memory.
      // Drops the post retrieve memory usage by about 20%
      System.gc();
    }
  }
}
