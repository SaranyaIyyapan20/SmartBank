package com.smartbank.Util;


import org.apache.commons.validator.routines.UrlValidator;

public class UrlValidationUtil {

    private static final UrlValidator validator =
            new UrlValidator(new String[]{"http", "https"});

    public static void validateUrl(String url) {
        if (url == null || !validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid URL format. Must start with http:// or https://");
        }
    }
}

