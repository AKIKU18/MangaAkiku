package com.example.mangav5.ScriptHelper;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;

public class isSourceAccessible {

    public boolean isSourceAccessible(String url) {
        try {
            // Use HEAD or GET; GET is safer if HEAD is blocked
            Jsoup.connect(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                            "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36")
                    .timeout(5000)
                    .get();
            return true; // accessible
        } catch (HttpStatusException e) {
            return e.getStatusCode() != 403; // false if forbidden
        } catch (Exception e) {
            return false; // network error, consider inaccessible
        }
    }
}
