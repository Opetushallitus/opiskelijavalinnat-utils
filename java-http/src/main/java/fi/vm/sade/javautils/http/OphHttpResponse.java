package fi.vm.sade.javautils.http;

import java.util.Optional;

/**
 * Handles the result of server request. Converts the result to the given type. Provides functionality for user to
 * handle error status codes.
 * Supports TEXT_PLAIN and APPLICATION_JSON content types.
 * @param <T> Type of the object returned at the end of a succesful server request.
 */
public interface OphHttpResponse<T> {
    /**
     * Separate handlers for given error status codes.
     * @param status Any number of status codes
     * @return Callback binder
     */
    OphHttpCallBack<T> handleErrorStatus(int... status);

    /**
     * Convert result to given type on status ccdes provided as argument.
     * @param status Any number of status codes
     * @return Server result converted to given type. Empty if error is handled by user.
     */
    Optional<T> expectedStatus(int... status);

}
