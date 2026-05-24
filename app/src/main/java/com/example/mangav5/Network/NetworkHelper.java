package com.example.mangav5.Network;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import okhttp3.OkHttpClient;
import java.util.concurrent.TimeUnit;

public class NetworkHelper {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36";
    public static final int DEFAULT_TIMEOUT = 30000;

    private static OkHttpClient okHttpClient;

    public static synchronized OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                    .readTimeout(DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build();
        }
        return okHttpClient;
    }

    public static Connection getJsoupConnection(String url) {
        return Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(DEFAULT_TIMEOUT)
                .followRedirects(true);
    }
}
