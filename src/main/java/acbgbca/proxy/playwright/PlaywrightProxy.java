package acbgbca.proxy.playwright;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response.ResponseBuilder;

@Path("/content")
public class PlaywrightProxy {

    private Logger log = LoggerFactory.getLogger(PlaywrightProxy.class);

    @PostConstruct
    public void init() {
        try (Playwright playwright = Playwright.create()) {
            log.info("Browsers downloaded");
        }
    }

    @GET
    public jakarta.ws.rs.core.Response proxyRequest(@QueryParam("url") String url) {
        
        Long startTime = System.currentTimeMillis();
        log.info("Retrieving content from location: {}", url);
        try (
            Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
        ) {
            // Don't download images
            page.route(Pattern.compile(".*\\.(jpg|gif|png)"), route -> {
                log.info("Aborting {}", route.toString());
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
            page.evaluate(String.format("""
                const element = document.createElement('base');
                element.href = '%s';
                document.head.insertBefore(element, document.head.firstChild);
                """, url));

            ResponseBuilder response = jakarta.ws.rs.core.Response.ok(page.content(), pageResponse.headerValue("Content-Type"));
            log.info("Retrieved content ins {} milliseconds", System.currentTimeMillis() - startTime);
            return response.build();
        } finally {
            // Force run a FULL GC cycle.
            // Without this Java will run a partial GC which doesn't reclaim as much memory.
            // Drops the post retrieve memory usage by about 20%
            System.gc();
        }
        
    }
    
}
