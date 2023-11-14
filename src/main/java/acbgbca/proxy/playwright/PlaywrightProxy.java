package acbgbca.proxy.playwright;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;

import jakarta.annotation.PostConstruct;

@RestController
public class PlaywrightProxy {

    private Logger log = LoggerFactory.getLogger(PlaywrightProxy.class);

    @PostConstruct
    public void init() {
        log.info("Starting init");
        try (Playwright playwright = Playwright.create()) {
            log.info("Browsers downloaded");
        } catch (Exception e) {
            log.error("Error setting up Playwright", e);
            System.exit(1);
        }
    }

    @GetMapping(value = "/", produces = MimeTypeUtils.TEXT_HTML_VALUE)
    public ResponseEntity<String> proxyRequest(@RequestParam("url") String url) {
        Long startTime = System.currentTimeMillis();
        log.info("Retrieving content from location: {}", url);
        try (
            Playwright playwright = Playwright.create();
            Browser browser = playwright.chromium().launch();
            Page page = browser.newPage();
        ) {
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

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
            headers.add(HttpHeaders.CONTENT_TYPE.toString(), pageResponse.headerValue(HttpHeaders.CONTENT_TYPE.toString()));
            headers.add(HttpHeaders.CONTENT_LOCATION, url);
            ResponseEntity<String> response = new ResponseEntity<String>(page.content(), headers, HttpStatusCode.valueOf(pageResponse.status()));
            
            log.info("Retrieved content ins {} milliseconds", System.currentTimeMillis() - startTime);
            return response;
        } finally {
            Runtime.getRuntime().gc();
        }
    }
    
}
