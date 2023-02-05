package com.github.vivyteam.url.api;

import com.github.vivyteam.url.api.contract.FullUrl;
import com.github.vivyteam.url.api.contract.ShortenedUrl;
import com.github.vivyteam.url.api.validator.URLValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class UrlApi {

    @Autowired
    URLValidator urlValidator;

    private final String LOCALHOST = "http://localhost:9000/";
    private final Map<String, String> shortToFullUrls = new HashMap<>();
    private final Map<String, String> fullToShortUrls = new HashMap<>();

    // The slash character is the URI standard path delimiter, so we soon find out that this returns a 404 if the PathVariable contains a slash.
    // So I made a change to PathVariable to RequestParam

    @GetMapping("/short")
    public Mono<ResponseEntity<ShortenedUrl>> shortUrl(@RequestParam("url") final String url) {

        if (!urlValidator.validateURL(url)) {
            return Mono.just(ResponseEntity.badRequest().build());
        }

        if (fullToShortUrls.containsKey(url)) {
            return Mono.just(ResponseEntity.ok(new ShortenedUrl(fullToShortUrls.get(url))));
        }

        var shortenedUrlId = UUID.randomUUID().toString().substring(0, 12);
        var shortenedUrl = LOCALHOST + shortenedUrlId;

        shortToFullUrls.put(shortenedUrlId, url);
        fullToShortUrls.put(url, shortenedUrl);

        return Mono.just(ResponseEntity.ok(new ShortenedUrl(shortenedUrl)));
    }

    @GetMapping("/full")
    public Mono<ResponseEntity<FullUrl>> getFullUrl(@RequestParam("url") final String url) {

        var shortenedUrlId = url.substring(url.lastIndexOf('/') + 1);
        var fullUrl = shortToFullUrls.get(shortenedUrlId);

        if (fullUrl == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        return Mono.just(ResponseEntity.ok(new FullUrl(fullUrl)));
    }

    @GetMapping
    public Mono<ResponseEntity<Void>> redirect(@RequestParam("url") final String url) {
        int urlStartingIndex = url.lastIndexOf('/') + 1;
        var shortenedUrlId = url.substring(urlStartingIndex);
        var fullUrl = shortToFullUrls.get(shortenedUrlId);

        if (fullUrl == null) {
            return Mono.just(ResponseEntity.notFound().build());
        }

        try {
            return Mono.just(ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT).location(URI.create(fullUrl)).build());
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
    }

}
