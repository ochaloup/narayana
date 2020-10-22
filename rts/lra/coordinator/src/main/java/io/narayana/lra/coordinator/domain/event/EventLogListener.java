package io.narayana.lra.coordinator.domain.event;

import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class EventLogListener {
    private static final Logger log = Logger.getLogger(EventLogListener.class);

    public void onLraInfoEvent(@Observes LRAInfoEvent infoEvent) {
        log.infof(">>>> %s", infoEvent);
    }
}
