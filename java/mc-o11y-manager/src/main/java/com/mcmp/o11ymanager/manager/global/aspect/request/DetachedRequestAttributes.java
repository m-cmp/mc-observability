package com.mcmp.o11ymanager.manager.global.aspect.request;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * A request scope for work that outlives its HTTP request. Request-scoped beans such as {@link
 * RequestInfo} fail outside a request thread, so background work that calls code using them runs
 * inside one of these, carrying over the original request id.
 */
public final class DetachedRequestAttributes implements RequestAttributes {

    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private final Map<String, Runnable> destructionCallbacks = new LinkedHashMap<>();

    /**
     * Runs {@code task} on the current thread with a fresh request scope holding {@code requestId}.
     */
    public static void run(RequestInfo requestInfo, String requestId, Runnable task) {
        DetachedRequestAttributes scope = new DetachedRequestAttributes();
        RequestContextHolder.setRequestAttributes(scope);
        try {
            requestInfo.setRequestId(requestId);
            task.run();
        } finally {
            RequestContextHolder.resetRequestAttributes();
            scope.destructionCallbacks.values().forEach(Runnable::run);
        }
    }

    @Override
    public Object getAttribute(String name, int scope) {
        return scope == SCOPE_REQUEST ? attributes.get(name) : null;
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        if (scope == SCOPE_REQUEST) {
            attributes.put(name, value);
        }
    }

    @Override
    public void removeAttribute(String name, int scope) {
        if (scope == SCOPE_REQUEST) {
            attributes.remove(name);
            destructionCallbacks.remove(name);
        }
    }

    @Override
    public String[] getAttributeNames(int scope) {
        return scope == SCOPE_REQUEST ? attributes.keySet().toArray(new String[0]) : new String[0];
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
        if (scope == SCOPE_REQUEST) {
            destructionCallbacks.put(name, callback);
        }
    }

    @Override
    public Object resolveReference(String key) {
        return null;
    }

    @Override
    public String getSessionId() {
        throw new UnsupportedOperationException("No session outside an HTTP request");
    }

    @Override
    public Object getSessionMutex() {
        throw new UnsupportedOperationException("No session outside an HTTP request");
    }
}
