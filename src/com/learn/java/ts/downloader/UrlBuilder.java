package com.learn.java.ts.downloader;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public interface UrlBuilder {
    List<URL> build(UrlConfig urlConfig);
}

class UrlBuilderImpl implements UrlBuilder {

    private static final String PREFIX_PATTERN = ".+segment";
    private static final String POSTFIX_PATTERN = ".ts.+";

    @Override
    public List<URL> build(UrlConfig urlConfig) {
        System.out.println("===================[ START TO BUILD URLs ]===================");

        var urls = new ArrayList<URL>();
        if (urlConfig == null) return urls;

        for(int i = urlConfig.start; i <= urlConfig.end; i++) {
            URL url = build(urlConfig.url, i);
            if (url != null) urls.add(url);
        }
        return urls;
    }

    private URL build(String sample, int index) {
        try {
            String firstPath = null;
            String secondPath = null;

            var regex = Pattern.compile(PREFIX_PATTERN);
            var matcher = regex.matcher(sample);
            if (matcher.find()) {
                firstPath = sample.substring(matcher.start(), matcher.end());
            }
            matcher = Pattern.compile(POSTFIX_PATTERN).matcher(sample);
            if (matcher.find()) {
                secondPath = sample.substring(matcher.start(), matcher.end());
            }
            return new URL(firstPath + index + secondPath);

        } catch (Exception e) {
            return null;
        }
    }
}
