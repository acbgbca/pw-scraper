package com.github.acbgbca.pwscraper;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Page.ScreenshotOptions;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import io.quarkus.runtime.util.StringUtil;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class PWScaper {

  public static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");
  public static final Set<String> NON_HTML_EXTENSIONS = Set.of("png", "jpg", "jpeg", "pdf");

  private Logger log = LoggerFactory.getLogger(PWScaper.class);

  @ConfigProperty(name = "browser.height")
  private Integer defaultHeight;

  @ConfigProperty(name = "browser.width")
  private Integer defaultWidth;

  @ConfigProperty(name = "browser.default")
  private Engine defaultBrowser;

  @PostConstruct
  public void init() {
    try (Playwright unused = Playwright.create()) {
      log.info("Browsers downloaded");
    }
  }

  @GET
  @Path("/content/{file}")
  @Deprecated()
  public jakarta.ws.rs.core.Response getImagePdf(
      @QueryParam("url") String url,
      @QueryParam("width") Integer widthParam,
      @QueryParam("height") Integer heightParam,
      @QueryParam("browser") Engine browserParam,
      @QueryParam("wait") Integer waitInSeconds,
      @QueryParam("waitSelector") String waitSelector,
      @PathParam("file") String filename) {
    return getHtml(
        url, widthParam, heightParam, browserParam, waitInSeconds, waitSelector, filename);
  }

  @GET
  @Path("/{file}")
  public jakarta.ws.rs.core.Response getHtml(
      @QueryParam("url") String url,
      @QueryParam("width") Integer widthParam,
      @QueryParam("height") Integer heightParam,
      @QueryParam("browser") Engine browserParam,
      @QueryParam("wait") Integer waitInSeconds,
      @QueryParam("waitSelector") String waitSelector,
      @PathParam("file") String filename) {
    Integer browserWidth = widthParam != null ? widthParam : defaultWidth;
    Integer browserHeight = heightParam != null ? heightParam : defaultHeight;
    String fileExtension =
        filename.indexOf('.') >= 0 ? filename.substring(filename.lastIndexOf('.')) : "";
    fileExtension = fileExtension.toLowerCase();

    Long startTime = System.currentTimeMillis();
    log.info("Retrieving content from location: {}", url);
    NewContextOptions contextOptions = new NewContextOptions();
    contextOptions.setScreenSize(browserWidth, browserHeight);
    contextOptions.setViewportSize(browserWidth, browserHeight);
    try (Playwright playwright = Playwright.create();
        Browser browser = getBrowserType(playwright, browserParam).launch();
        BrowserContext context = browser.newContext(contextOptions);
        Page page = context.newPage(); ) {

      if (!NON_HTML_EXTENSIONS.contains(fileExtension)) {
        // Don't download images
        page.route(
            Pattern.compile(".*\\.(jpg|gif|png)"),
            route -> {
              log.debug("Aborting {}", route.toString());
              route.abort();
            });
      }

      // Load page and wait for content to load
      Response pageResponse = page.navigate(url);
      page.waitForLoadState(LoadState.NETWORKIDLE);

      if (waitInSeconds != null) {
        try {
          log.debug("Waiting for {} seconds", waitInSeconds);
          TimeUnit.SECONDS.sleep(waitInSeconds.longValue());
          page.waitForLoadState(LoadState.NETWORKIDLE);
        } catch (InterruptedException e) {
          // Ignore
          log.warn("Exception during wait", e);
        }
      }

      if (!StringUtil.isNullOrEmpty(waitSelector)) {
        page.waitForSelector(waitSelector);
      }

      switch (fileExtension.toLowerCase()) {
        case "png":
        case "jpg":
        case "jpeg":
          return getImage(page, pageResponse, fileExtension);
        case "pdf":
        default:
          return getHtml(page, pageResponse);
      }
    } finally {
      log.info("Retrieved content ins {} milliseconds", System.currentTimeMillis() - startTime);
      // Force run a FULL GC cycle.
      // Without this Java will run a partial GC which doesn't reclaim as much memory.
      // Drops the post retrieve memory usage by about 20%
      System.gc();
    }
  }

  public jakarta.ws.rs.core.Response getHtml(Page page, Response pageResponse) {
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
            page.url()));
    ResponseBuilder response =
        jakarta.ws.rs.core.Response.ok(page.content(), pageResponse.headerValue("Content-Type"));
    return response.build();
  }

  public jakarta.ws.rs.core.Response getImage(Page page, Response pageResponse, String extension) {
    ScreenshotOptions options = new ScreenshotOptions();
    String mimeType;
    options.setFullPage(true);
    if ("png".equals(extension)) {
      options.setType(ScreenshotType.PNG);
      mimeType = "image/png";
    } else {
      options.setType(ScreenshotType.JPEG);
      mimeType = "image/jpeg";
    }

    ResponseBuilder response = jakarta.ws.rs.core.Response.ok(page.screenshot(options), mimeType);

    return response.build();
  }

  public jakarta.ws.rs.core.Response getPdf(Page page, Response pageResponse) {
    ResponseBuilder response = jakarta.ws.rs.core.Response.ok(page.pdf(), "application/pdf");
    return response.build();
  }

  private BrowserType getBrowserType(Playwright playwright, Engine browserParam) {
    Engine engine = browserParam != null ? browserParam : defaultBrowser;

    switch (engine) {
      case CHROMIUM:
        return playwright.chromium();
      case FIREFOX:
        return playwright.firefox();
      case WEBKIT:
        return playwright.webkit();
      default:
        return playwright.chromium();
    }
  }
}
