package fi.vm.sade.javautils.http;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OphHttpOnErrorCallBackImpl<T> implements OphHttpOnErrorCallBack<T> {
    private Set<Integer> statusCodes;

    private OphHttpResponse<T> ophHttpResponse;

    private Function<String, Optional<T>> callBack;

    OphHttpOnErrorCallBackImpl(int[] statusCodes, OphHttpResponse<T> ophHttpResponse) {
        this.statusCodes = Arrays.stream(statusCodes)
                .boxed()
                .collect(Collectors.toSet());
        this.ophHttpResponse = ophHttpResponse;
    }

    Set<Integer> getStatusCode() {
        return this.statusCodes;
    }

    Function<String, Optional<T>> getCallBack() {
        return this.callBack;
    }

    @Override
    public OphHttpResponse<T> with(Function<String, Optional<T>> callBack) {
        this.callBack = callBack;
        return this.ophHttpResponse;
    }
}
