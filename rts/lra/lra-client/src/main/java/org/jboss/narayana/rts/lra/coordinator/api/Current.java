package org.jboss.narayana.rts.lra.coordinator.api;


import javax.ws.rs.core.MultivaluedMap;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import static org.jboss.narayana.rts.lra.coordinator.api.LRAClient.LRA_HTTP_HEADER;

public class Current {
    //    private static final ThreadLocal<Optional<Current>> lraContext = new ThreadLocal<>();
    private static final ThreadLocal<Current> lraContexts = new ThreadLocal<>();

    private Stack<URL> stack;
    private Map<String, Object> state;

    private Current(URL url) {
        stack = new Stack<>();
        stack.push(url);
    }

    public static Object putState(String key, Object value) {
        Current current = lraContexts.get();

        if (current != null)
            return current.updateState(key, value);

        return null;
    }

    public static Object getState(String key) {
        Current current = lraContexts.get();

        if (current != null && current.state != null)
            return current.state.get(key);

        return null;
    }

    public Object updateState(String key, Object value) {
        if (state == null)
            state = new HashMap<>();

        return state.put(key, value);
    }

    private static void clearContext(Current current) {
        if (current.state != null)
            current.state.clear();

        lraContexts.set(null);
    }

    public static URL peek() {
        Current current = lraContexts.get();

        return current != null ? current.stack.peek() : null;
    }

    public static URL pop() {
        Current current = lraContexts.get();
        URL lraId = null;

        if (current != null) {
            lraId = current.stack.pop(); // there must be at least one

            if (current.stack.empty())
                clearContext(current);
        }

        return lraId;
    }


    public static boolean pop(URL lra) {
        Current current = lraContexts.get();

        // NB URIs would have been preferable to URLs for testing equality
        if (current == null || !current.stack.contains(lra))
            return false;

        current.stack.remove(lra);

        if (current.stack.empty())
            clearContext(current);

        return true;
    }

    /**
     * push the current context onto the stack of contexts for this thread
     * @param lraId id of context to push (must not be null)
     */
    public static void push(URL lraId) {
        Current current = lraContexts.get();

        if (current == null) {
            lraContexts.set(new Current(lraId));
        } else {
            if (!current.stack.peek().equals(lraId))
                current.stack.push(lraId);
        }
    }

    public static void updateLRAContext(MultivaluedMap<String, Object> headers) {
        URL lraId = Current.peek();

        if (lraId != null)
            headers.putSingle(LRA_HTTP_HEADER, lraId);
        else
            headers.remove(LRA_HTTP_HEADER);
    }

    public static void popAll() {
        lraContexts.set(null);
    }

    public static void clearContext(MultivaluedMap<String, String> headers) {
        headers.remove(LRA_HTTP_HEADER);
        popAll();
    }

    public static void updateLRAContext(URL lraId, MultivaluedMap<String, String> headers) {
        headers.putSingle(LRA_HTTP_HEADER, lraId.toString());
        push(lraId);
    }


/*    static Optional<URL> getCurrent() {
        Optional<Stack<URL>> url = lraContext.get().map(Current::getStack).filter(st -> !st.empty());

        return Optional.ofNullable(url.isPresent() ? url.get().peek() : null);
    }


    private static void setCurrent(URL current) {
        Optional<Current> o = Optional.of(new Current(current));

        if (lraContext.get() == null)
            lraContext.set(o);
        else
            lraContext.get().ifPresent(c -> c.stack.push(current));
    }*/

}
