package fi.vm.sade.javautils.http;

import java.util.Optional;
import java.util.function.Function;

/**
 * Binds status cede(s) and callback function of OphHttpResponse.
 */
interface OphHttpCallBack<T> {
    /**
     * Bind callback method to provided status code.
     * @param callBack Function that gets server error message as argument and returns Optional that will be returned
     *                 by OphHttpResponse.expectedStatus() in case exception is handled by this callBack.
     * @return Response for continue this builder chain
     */
    OphHttpResponse<T> with(Function<String, Optional<T>> callBack);
}
