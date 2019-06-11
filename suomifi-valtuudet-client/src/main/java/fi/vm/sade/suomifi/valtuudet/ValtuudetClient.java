package fi.vm.sade.suomifi.valtuudet;

/**
 * Http client to use suomi.fi-valtuudet service.
 *
 * <p>Prerequisites:</p>
 * <ul>
 *     <li>Get credentials to service ({@link ValtuudetProperties})</li>
 *     <li>Create endpoint (= callbackUrl) which expects query parameter named 'code'</li>
 * </ul>
 *
 * <p>Typical flow:</p>
 * <ol>
 *     <li>Create session to service with {@link #createSession(ValtuudetType, String)}</li>
 *     <li>Redirect user to {@link #getRedirectUrl(String, String, String)}.</li>
 *     <li>(user returns to callbackUrl)</li>
 *     <li>Get access token by code given to callback endpoint {@link #getAccessToken(String, String)}</li>
 *     <li>
 *         Get person or organisation selected by user depending on type used in session
 *         ({@link #getSelectedPerson(String, String)} or {@link #getSelectedOrganisation(String, String)})
 *     </li>
 *     <li>Destroy session from service with {@link #destroySession(ValtuudetType, String)}</li>
 * </ol>
 */
public interface ValtuudetClient {

    /**
     * Create session to service.
     *
     * @param type person or organisation
     * @param nationalIdentificationNumber user's national identification number
     * @return session
     */
    SessionDto createSession(ValtuudetType type, String nationalIdentificationNumber);

    /**
     * Destroy session from service.
     *
     * @param type person or organisation
     * @param sessionId session id from {@link #createSession(ValtuudetType, String)}
     */
    void destroySession(ValtuudetType type, String sessionId);

    /**
     * Returns url to which user should be redirected.
     *
     * @param userId user id from {@link #createSession(ValtuudetType, String)}
     * @param callbackUrl user is redirected to this url after selection
     * @param language "fi"|"sv"|"en"
     * @return redirect url
     */
    String getRedirectUrl(String userId, String callbackUrl, String language);

    /**
     * Returns access token by code given to callback url.
     *
     * @param code code given to callback url
     * @param callbackUrl original callback url
     * @return access token
     */
    String getAccessToken(String code, String callbackUrl);

    /**
     * Returns person user selected. This should be used only when {@link ValtuudetType#PERSON} was used in register.
     *
     * @param sessionId session id from {@link #createSession(ValtuudetType, String)}
     * @param accessToken access token from {@link #getAccessToken(String, String)}
     * @return person
     */
    PersonDto getSelectedPerson(String sessionId, String accessToken);

    /**
     * Returns if user is authorized as person.
     *
     * @param sessionId session id from {@link #createSession(ValtuudetType, String)}
     * @param accessToken access token from {@link #getAccessToken(String, String)}
     * @param nationalIdentificationNumber person's national identification number
     * @return true if user is authorized as person, false otherwise
     */
    boolean isAuthorizedToPerson(String sessionId, String accessToken, String nationalIdentificationNumber);

    /**
     * Returns organisation user selected. This should be used only when {@link ValtuudetType#ORGANISATION} was used in
     * register.
     *
     * @param sessionId session id from {@link #createSession(ValtuudetType, String)}
     * @param accessToken access token from {@link #getAccessToken(String, String)}
     * @return organisation
     */
    OrganisationDto getSelectedOrganisation(String sessionId, String accessToken);

}
