# PW Scraper

A service that uses Playwright to download pages that are rendered using React or other Javascript framework.

Work in progress.


# Development

Run a local nginx with the test site.

```
docker run -it --rm -p 6080:80 --name web -v ./src/test/resources/nginx/www:/usr/share/nginx/html nginx
```