package org.jboss.narayana.rts.lra.filter;

import java.net.URL;

class FilterState {
    private static final ThreadLocal<FilterState> lraContext = new ThreadLocal<>();

    URL id;
    URL newLRA;
    URL suspendedLRA;
    String recoveryUrl;

    FilterState(URL lraId, URL newLRA, URL suspendedLRA, String recoveryUrl) {
        this.id = lraId;
        this.newLRA = newLRA;
        this.suspendedLRA = suspendedLRA;
        this.recoveryUrl = recoveryUrl;
    }

    static FilterState getCurrentLRA() {
        return lraContext.get();
    }

    static void setCurrentLRA(URL lra) {
        lraContext.set(new FilterState(lra, null, null, null));
    }

    static void setCurrentLRA(FilterState filterState) {
        lraContext.set(filterState);
    }

    static FilterState clearCurrentLRA() {
        FilterState state = lraContext.get();

        lraContext.set(null);

        return state;
    }
}
