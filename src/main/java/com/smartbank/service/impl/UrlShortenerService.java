package com.smartbank.service.impl;

import com.smartbank.Util.Base62;
import com.smartbank.Util.UrlValidationUtil;
import com.smartbank.entity.UrlMapping;
import com.smartbank.repository.UrlMappingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UrlShortenerService {

    private final UrlMappingRepository repository;

    public UrlShortenerService(UrlMappingRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public String shortenUrl(String longUrl, LocalDateTime expiresAt) {
        UrlValidationUtil.validateUrl(longUrl);

        UrlMapping entity = new UrlMapping();
        entity.setLongUrl(longUrl);
        entity.setExpiresAt(expiresAt);
        entity.setActive(true);
        repository.save(entity);

        // Generate short code from ID (Base62)
        String shortCode = Base62.encode(entity.getId());
        entity.setShortCode(shortCode);
        repository.save(entity);

        return shortCode;
    }

    public String resolveUrl(String shortCode) {
        Optional<UrlMapping> optional = repository.findByShortCode(shortCode);

        if (optional.isEmpty()) {
            throw new RuntimeException("Short URL not found");
        }

        UrlMapping mapping = optional.get();

        if (!mapping.isActive() ||
                (mapping.getExpiresAt() != null && mapping.getExpiresAt().isBefore(LocalDateTime.now()))) {
            throw new RuntimeException("Short URL expired or inactive");
        }

        return mapping.getLongUrl();
    }
}

