package com.smartbank.controller;

import com.smartbank.service.impl.UrlShortenerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/URLShortener")
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<?> shorten(@RequestBody Map<String, String> request) {
        String longUrl = request.get("longUrl");
        String expiresAtStr = request.get("expireAt");

        LocalDateTime expiresAt = null;
        if (expiresAtStr != null) {
            expiresAt = LocalDateTime.parse(expiresAtStr);
        }

        String shortCode = service.shortenUrl(longUrl, expiresAt);
        String shortUrl = "https://sho.rt/" + shortCode;

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "shortUrl", shortUrl,
                "shortCode", shortCode
        ));
    }

    @GetMapping("/{shortCode}")
    public RedirectView redirect(@PathVariable String shortCode) {
        String longUrl = service.resolveUrl(shortCode);
        RedirectView redirectView = new RedirectView();
        redirectView.setUrl(longUrl);
        return redirectView;
    }
}
