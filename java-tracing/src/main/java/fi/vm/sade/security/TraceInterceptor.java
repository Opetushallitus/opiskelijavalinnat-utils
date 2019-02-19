package fi.vm.sade.security;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Poor man's performance tracer, investigate Spring etc performance interceptors if this are used beyond temp usage
 *
 * @author Antti Salonen
 */
public class TraceInterceptor implements MethodInterceptor {

    private static Map<String, Long> cumulativeInvocationTimes = new HashMap<String, Long>();
    private static Map<String, Long> invocationCounts = new HashMap<String, Long>();

    private final static Logger logger = LoggerFactory.getLogger(TraceInterceptor.class);

    @Override
    public Object invoke(MethodInvocation methodInvocation) throws Throwable {
        long t0 = System.currentTimeMillis();
        try {
            return methodInvocation.proceed();
        } finally {
            if(logger.isTraceEnabled()) {
                long invocationTime = System.currentTimeMillis() - t0;
                String methodKey = methodInvocation.getMethod().getDeclaringClass().getSimpleName()+"."+methodInvocation.getMethod().getName();
                Long cumulativeTime = addCumulativeTime(invocationTime, methodKey);
                Long count = raiseInvocationCount(methodKey);
                logger.trace("PERFORMANCE TRACE, cumulative: {} ms, count: {}, method: {}", new Object[]{cumulativeTime, count, methodKey});
            }
        }
    }

    private synchronized static Long addCumulativeTime(long invocationTime, String methodKey) {
        Long cumulativeInvocationTime = cumulativeInvocationTimes.get(methodKey);
        if (cumulativeInvocationTime == null) {
            cumulativeInvocationTime = 0l;
        }
        cumulativeInvocationTime += invocationTime;
        cumulativeInvocationTimes.put(methodKey, cumulativeInvocationTime);
        return cumulativeInvocationTime;
    }

    private synchronized static Long raiseInvocationCount(String methodKey) {
        Long count = invocationCounts.get(methodKey);
        if (count == null) {
            count = 0l;
        }
        count ++;
        invocationCounts.put(methodKey, count);
        return count;
    }

}
