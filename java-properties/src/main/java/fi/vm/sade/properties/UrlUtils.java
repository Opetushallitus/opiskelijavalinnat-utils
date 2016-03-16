package fi.vm.sade.properties;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class UrlUtils {
    public static String joinUrl(String... urls) {
        if (urls.length == 0) {
            throw new RuntimeException("no arguments");
        }
        String url = null;
        for (String arg : urls) {
            if (url == null) {
                url = arg;
            } else {
                boolean endsWith = url.endsWith("/");
                boolean startsWith = arg.startsWith("/");
                if(endsWith && startsWith) {
                    url = url + arg.substring(1);
                } else if (endsWith || startsWith) {
                    url = url + arg;
                } else {
                    url = url + "/" + arg;
                }
            }
        }
        return url;
    }

    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8")
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
