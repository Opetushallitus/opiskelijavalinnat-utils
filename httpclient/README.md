# Common HTTP interface for OPH

    Koulutus koulutus = client.get("tarjonta-service.koulutus", koulutusId).expectStatus(200).
                            execute( r -> mapper.readValue(r.asInputStream(), new GenericType<Koulutus>()) );

    Koulutus newKoulutus = new Koulutus()
    Koulutus savedKoulutus = client.post("tarjonta-service.koulutus", koulutusId).expectStatus(200).
                            dataWriter("application/json", "UTF-8", out -> mapper.writeValue(out, newKoulutus) ).
                            execute( r -> mapper.readValue(r.asInputStream(), new GenericType<Koulutus>()) );

# Features

* Simple fluent API
* Easy streaming for request (chunked, streaming) and response (streaming reads) -> one liner handlers and small memory use by default
* Built in assertions for response status `expectStatus(200,...)` and content type `accept(JSON)`.
By default the client expects the status code to be 2xx.
* Built in retry: `retryOnError(times, delayMs)`
* Uses Apache Httpclient 4.5.2, but you can write an adapter for other http client libraries.
  Just implement your own OphHttpClientProxy, OphHttpClientProxyRequest and OphHttpResponse
* Supports OPH's CSRF protection and clientSubSystem

# Usage

## Maven

    <dependency>
        <groupId>fi.vm.sade.java-utils</groupId>
        <artifactId>httpclient</artifactId>
        <version>0.1.0-SNAPSHOT</version>
    </dependency>

## SBT

    "fi.vm.sade.java-utils" %% "httpclient" % "0.1.0-SNAPSHOT"

## Initializing a client

Initialize a non-caching http client that pools connections.

    OphHttpClient client = ApacheOphHttpClient.createDefaultOphHttpClient("tester", properties, 10000, 600);

* `tester` is clientSubSystemCode header, which identifies this client by adding the header with the specified value to the request
* `properties` is a OphProperties instance. Urls are resolved through it, see: https://github.com/Opetushallitus/java-utils/tree/master/java-properties
* 10000ms is a common timeout for various timeout values
* 600s is the how long each connection is up

`ApacheHttpClientBuilder.createCustomBuilder()` can be used to configure your own HttpClient.
See available methods in `ApacheHttpClientBuilder`

    ApacheHttpClientBuilder builder = ApacheOphHttpClient.createCustomBuilder().
                            createCachingClient( 50 * 1000, 10 * 1024 * 1024).
                            setDefaultConfiguration(10000, 60);
    builder.httpBuilder.setProxy(...); // Accessing the original HttpClientBuilder's method
    OphHttpClient cachingClient = new OphHttpClient(builder.build(), "tester", properties)

By default initialized clients:
* accept the response if the response status code is between 200 and 299. Otherwise an exception is thrown.
* follow redirects automatically
* don't retry automatically

## Making requests

A regular GET for JSON

1. Resolve url. Url is resolved from OphProperties instances and koulutusId is filled in to the url.
2. Make the request. Request is made with header: Accept: application/json
3. Verify that response code is 200 and Content-Type matches Accept
4. Handle the response with a OphHttpResponseHandler instance or lambda. The handler gets an instance of OphHttpResponse which
provides methods related to the response.
5. Connection and any related resources are released after handler is finished.

        Koulutus koulutus = client.get("tarjonta-service.koulutus", koulutusId).expectStatus(200).accept(JSON).
            execute(r -> mapper.readValue(r.asInputStream(), Koulutus.class));

Make a POST and verify that the response code is 200. You can use the plain execute() method without writing a handler.
All dataWriter() content is sent as chunked. note: ApacheHttpClientBuilder uses a 128k buffer so writing is buffered.

    client.post("tarjonta-service.koulutus").expectStatus(200).
        dataWriter("application/json", "UTF-8", out -> mapper.writeValue(out, koulutus) )
        execute();
        
Make a x-www-form-urlencoded POST

    client.post("tarjonta-service.koulutus").expectStatus(200).
        dataWriter(FORM_URLENCODED, UTF8, out -> OphHttpClient.formUrlEncodedWriter(out).param("service", service).param("size",1) )
        execute();

If you want to handle exceptions with your own code, you can use onError

    // handle the error somehow and throw and the original exception
    String responseBody = client.get("local.test")
        .onError((requestParameters, response, exception) -> {logger.error("There was an error!", exception); throw exception;})
        .execute(response -> response.asText())
        
    // handle the error somehow and do not throw an exception but instead return a default value
    String responseBody = client.get("local.test")
        .onError((requestParameters, response, exception) -> {logger.error("There was an error!", exception); return null;})
        .execute(response -> response.asText())

* note 1: It should either throw an exception or return an object of correct type (= of the same type as execute returns). Otherwise you'll get a class cast exception if the code uses the returned value.
* note 2: errorHandler.handleError() needs to handle cases where response is null.
* note 3: If your execute method throws an exception by itself, onError will catch it.
* note 4: Per request, there can be only one onError handler.

There is also `handleManually()` which doesn't release anything. It returns an OphHttpResponse instance which you need to close.

    OphHttpResponse response = client.get("tarjonta-service.koulutus", koulutusId).
        expectStatus(200).accept(JSON).handleManually();
    try {
        // do something
        response.asInputStream();
    } finally {
        response.close();
    }

## Retrying

Make the request and handle the response. Retry 3 times (and wait 2000ms between requests) if anything throws an exception during the process.
Warning: The HTTP request is also repeated, so you might get multiple requests even if the original request succeeds but the assertions or
the handler throw an exception.

    Koulutus koulutus = client.get("tarjonta-service.koulutus", koulutusId).
        expectStatus(200).accept(JSON).
        retryOnError(3, 2000).
        execute(r -> mapper.readValue(r.asInputStream(), new GenericType<Koulutus>()));
