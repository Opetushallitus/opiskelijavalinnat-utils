
Java CAS Client
===============

This is a library with two main functionalities:
 1. Making API calls to CAS authenticated services (wrapping AsyncHttpClient with authentication headers)
 2. Checking CAS tickets from calls that are made to us.

The CAS dance
-------------

From API user point of view, CAS authentication has the following phases:
 1. Exchange credentials (username&password) for a ticket-granting ticket (TGT) in CAS server
   * once a day or so
 2. Exchange ticket-granting ticket (TGT) for a service ticket (ST) in CAS server 
   * every time making a call to a new service
   * service tickets are short-lived, by default only five seconds
 3. Exchange service ticket (ST) for a session cookie in the service that has the API you're calling
   * this is not really part of CAS
   * it is only useful because sessions are more long-lived that service tickets
   * but all opintopolku services support sessions
   * some opintopolku services even have APIs where you cannot authenticate with service tickets but you need a session instead
 4. Call the API you actually need with that session cookie
   * most services issue session cookies anyway whether the service ticket was valid or not
   * so if the authentication was unsuccessful, at this stage the session is not associated with any credentials and you will likely get a 403 or 302 pointing to a login screen

Checking service tickets
------------------------

From API provider point of view, using CAS is pretty straightforward.

When processing calls made into your service, you may get a service ticket (ST) that you need to check for validity.  This happens in step three of the above CAS dance, but the user of the API doesn't know what the service is doing with the service ticket.
 1. service calls the CAS service with the service ticket (ST) it got in a header / parameter
 2. service parses authorization details (such as username, roles, groups) from the response of CAS server
   * the CAS server is a trusted source of authority
 3. service then associates this information with the client's session so that it is available in the future with the session cookie
   * again, this is not part of CAS, you could require service tickets for every call

Using Java CAS client
---------------------

You need a separate CAS client for every service you want to call.  The creation of the CAS client looks like:
```
import fi.vm.sade.javautils.nio.cas.CasClient;
import fi.vm.sade.javautils.nio.cas.CasClientBuilder;
[...]
CasClient client = CasClientBuilder.build(
	new CasConfig.CasConfigBuilder(
		username,
		password,
		casServerUrl,
		serviceBaseUrl,
		csrfToken,
		callerIdentifier,
		serviceAuthenticationUrlSuffix)
	.setJsessionName(sessionCookieName)
	.build())
```

Here, the bits of information are:
 * username, password: the credentials that are given to the CAS server to get a TGT.
 * casServerUrl: where the CAS server can be found.
   * This is something like `https://virkailija.testiopintopolku.fi/cas`
   * used for TGT fetching, appending `v1/tickets` to it for the call
   * also used for ST checking (if you do that), appending `/serviceValidate` to it
 * serviceBaseUrl: where the API service you want to call can be found.  
   * This is something like `https://virkailija.testiopintopolku.fi/service-we-need-to-call`
   * It is passed on to the CAS server when fetching a ST
   * It is usually the common URL prefix for all the APIs within a single service
 * csrfToken: basically any text, no need to randomise.
 * callerIdentifier: any text, is supposed to identify who's making the call (for logging etc)
 * serviceAuthenticationUrlSuffix: what to append to serviceBaseUrl when changing ST for session
 * sessionCookieName: which cookie to save for making further calls
   * these last two are service specific (and not mandated by CAS).
   * for Spring, they are usually `/j_spring_cas_security_check` and `JSESSIONID` respectively.
   * for Clojure, they might be `/auth/cas` and `ring-session` or whatever.
   * in general, the only way to know is to look at the source of the other service to see what it is using for sessions and which endpoints process service tickets.

The created client can be used for making any API calls (but if they are not inside the serviceBaseUrl, they will likely fail authentication).  You build a AsyncHttpClient Request object and give it to client.execute().  For details on how AHC requests work, consult [its documentation](https://github.com/AsyncHttpClient/async-http-client/blob/main/README.md#sending-requests).

Building and testing
--------------------

To run the tests: `mvn test`
To build: `mvn package`
