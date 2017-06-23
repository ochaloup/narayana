package org.jboss.narayana.rts.lra.filter;

class FilterState {
    private static final ThreadLocal<FilterState> lraContext = new ThreadLocal<>();

    String id;
    String newLRA;
    String suspendedLRA;

    FilterState(String lraId, String newLRA, String suspendedLRA) {
        this.id = lraId;
        this.newLRA = newLRA;
        this.suspendedLRA = suspendedLRA;
    }

    static FilterState getCurrentLRA() {
        return lraContext.get();
    }

    static void setCurrentLRA(String lra) {
        lraContext.set(new FilterState(lra, null, null));
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
