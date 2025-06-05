package com.batch.android.core.domain;

public enum DomainService {
    WEB,
    METRIC;

    String url(String domain) {
        String fullDomain;
        switch (this) {
            case WEB:
                fullDomain = "ws.%s";
                break;
            case METRIC:
                fullDomain = "wsmetrics.%s/api-sdk";
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this);
        }

        return "https://" + String.format(fullDomain, domain);
    }
}
