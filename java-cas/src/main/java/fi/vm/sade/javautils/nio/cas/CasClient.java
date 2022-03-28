package fi.vm.sade.javautils.nio.cas;

import org.asynchttpclient.Request;
import org.asynchttpclient.Response;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface CasClient {

    CompletableFuture<Response> execute(Request request);

    CompletableFuture<String> validateServiceTicketWithVirkailijaUsername(String service, String ticket);

    CompletableFuture<HashMap<String, String>> validateServiceTicketWithOppijaAttributes(String service, String ticket);

    default HashMap<String, String> validateServiceTicketWithOppijaAttributesBlocking(String service, String ticket) throws ExecutionException, InterruptedException {
        return validateServiceTicketWithOppijaAttributes(service, ticket).get();
    }

    default Response executeBlocking(Request request) throws ExecutionException, InterruptedException {
        return execute(request).get();
    }

    default String validateServiceTicketWithVirkailijaUsernameBlocking(String service, String ticket) throws ExecutionException, InterruptedException {
        return validateServiceTicketWithVirkailijaUsername(service, ticket).get();
    }

}
