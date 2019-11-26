package fi.vm.sade.authorization;

import fi.vm.sade.javautils.httpclient.apache.ApacheOphHttpClient;
import fi.vm.sade.javautils.httpclient.OphHttpClient;
import fi.vm.sade.javautils.httpclient.OphHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class OrganizationOidProvider {
    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());
    public static final int MAX_CACHE_SIZE = 10000;

    private String organisaatioServiceUrl;
    private String rootOrganisaatioOid;
    private String callerId;

    private static Map<String,List<String>> cache = Collections.synchronizedMap(
            new LinkedHashMap<String, List<String>>(MAX_CACHE_SIZE + 1, .75F, true) {
                public boolean removeEldestEntry(Map.Entry eldest) {
                    return size() > MAX_CACHE_SIZE;
                }
            });

    protected OrganizationOidProvider() {}

    public OrganizationOidProvider(String rootOrganisaatioOid, String organisaatioServiceUrl, String callerId) {
        this.organisaatioServiceUrl = organisaatioServiceUrl;
        this.rootOrganisaatioOid = rootOrganisaatioOid;
        this.callerId = callerId;
    }

    public List<String> getSelfAndParentOidsCached(String targetOrganisationOid) {
        String cacheKey = targetOrganisationOid;
        List<String> cacheResult = cache.get(cacheKey);
        if (cacheResult == null) {
            cacheResult = getSelfAndParentOids(targetOrganisationOid);
            cache.put(cacheKey, cacheResult);
        }
        return cacheResult;
    }

    public List<String> getSelfAndParentOids(String organisaatioOid) {
        try {
            String url = organisaatioServiceUrl + "/rest/organisaatio/" + organisaatioOid + "/parentoids";
            String result = httpGet(url, 200);
            return Arrays.asList(result.split("/"));
        } catch (Exception e) {
            LOGGER.warn("Failed to getSelfAndParentOids, exception: " + e.getMessage() + ", returning only rootOrganisaatioOid and organisaatioOid", e);
            return Arrays.asList(rootOrganisaatioOid, organisaatioOid);
        }
    }

    private String httpGet(String url, int expectedStatus) {
        OphHttpClient client = new OphHttpClient(ApacheOphHttpClient.createCustomBuilder().
                createClosableClient().
                setDefaultConfiguration(10000, 60).build(), "OrganisaatioOidProvider");
        client.setCallerId(callerId);
        return client.get(url).execute((OphHttpResponse response) -> {
            if(expectedStatus != response.getStatusCode()) {
                throw new RuntimeException("Failed to call '" + url + "', invalid status: " + response.getStatusCode());
            }
            return response.asText();
        });
    }
}
