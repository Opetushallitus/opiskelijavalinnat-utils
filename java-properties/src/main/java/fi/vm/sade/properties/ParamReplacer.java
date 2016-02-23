package fi.vm.sade.properties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ParamReplacer {
    String replaceParams(String url, Object... params) {
        String queryString = "";
        for (int i = params.length; i > 0; i--) {
            Object param = params[i - 1];
            if (param instanceof Map) {
                Map paramMap = (Map) param;
                for (Object key : paramMap.keySet()) {
                    Object value = paramMap.get(key);
                    for(Object o: value instanceof List ? (List)value: Arrays.asList(value) ) {
                        String str = enc(o);
                        String keyString = enc(key);
                        String tmpUrl = url.replace("$" + keyString, str);
                        if (o != null && tmpUrl.equals(url)) {
                            queryString = extraParam(queryString, keyString, str);
                        }
                        url = tmpUrl;
                    }
                }
            } else {
                url = url.replace("$" + i, enc(param));
            }
        }
        return url + queryString;
    }

    String extraParam(String queryString, String keyString, String value) {
        return "";
    }

    String enc(Object param) {
        return param == null ? "" : param.toString();
    }
}
