package com.github.vivyteam.url.api;

import com.github.vivyteam.url.api.contract.FullUrl;
import com.github.vivyteam.url.api.contract.ShortenedUrl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureWebTestClient
public class UrlApiIntegrationTest {

    private final String LOCALHOST = "http://localhost:9000/";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void shortUrl_validUrl_returnsShortenedUrl() {
        var url = "https://www.example.com";

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/short").queryParam("url", url).build()).exchange().expectStatus().isOk().expectBody(ShortenedUrl.class).consumeWith(response -> {
            var shortenedUrl = response.getResponseBody();
            assertThat(shortenedUrl.getUrl()).startsWith(LOCALHOST);
        });
    }

    @Test
    public void shortUrl_invalidUrl_returnsBadRequest() {
        var url = "not a url";

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/short").queryParam("url", url).build()).exchange().expectStatus().isBadRequest();
    }

    @Test
    public void shortUrl_validUrl_returnsExistingShortenedUrl() {
        var url = "https://www.example.com";

        var shortenedUrl = webTestClient.get().uri(uriBuilder -> uriBuilder.path("/short").queryParam("url", url).build()).exchange().expectStatus().isOk().expectBody(ShortenedUrl.class).returnResult().getResponseBody();

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/short").queryParam("url", url).build()).exchange().expectStatus().isOk().expectBody(ShortenedUrl.class).consumeWith(response -> {
            assertEquals(shortenedUrl.getUrl(), response.getResponseBody().getUrl());
            assertThat(shortenedUrl.getUrl()).startsWith(LOCALHOST);
        });
    }


    @Test
    public void getFullUrl_validShortUrl_returnsFullUrl() {
        var url = "https://www.example.com";

        var shortenedUrl = webTestClient.get().uri(uriBuilder -> uriBuilder.path("/short").queryParam("url", url).build()).exchange().expectStatus().isOk().expectBody(ShortenedUrl.class).returnResult().getResponseBody();

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/full").queryParam("url", shortenedUrl.getUrl()).build()).exchange().expectStatus().isOk().expectBody(FullUrl.class).consumeWith(response -> {
            assertEquals(url, response.getResponseBody().getUrl());
        });
    }

    @Test
    public void getFullUrl_invalidShortUrl_returnsFullUrl() {
        var shortenedUrl = "not a url";
        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/full").queryParam("url", shortenedUrl).build()).exchange().expectStatus().isNotFound();
    }

    @Test
    public void redirect_shortUrl() {
        var url = "https://www.example.com";

        var shortenedUrl = webTestClient.get().uri(uriBuilder -> uriBuilder.path("/short").queryParam("url", url).build()).exchange().expectStatus().isOk().expectBody(ShortenedUrl.class).returnResult().getResponseBody();

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/").queryParam("url", shortenedUrl.getUrl()).build()).exchange().expectStatus().isPermanentRedirect().expectHeader().valueEquals("Location", url);

    }

    @Test
    public void redirect_invalidShortUrl() {
        var shortenedUrl = "https://www.example.com";

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/").queryParam("url", shortenedUrl).build()).exchange().expectStatus().isNotFound();
    }

}
