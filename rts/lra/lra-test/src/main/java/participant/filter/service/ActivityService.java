package participant.filter.service;

import participant.filter.model.Activity;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class ActivityService {
    private Map<String, Activity> activities = new HashMap<>();

    public Activity getActivity(String txId) throws NotFoundException {
        if (!activities.containsKey(txId))
            throw new NotFoundException(Response.status(404).entity("Invalid activity id: " + txId).build());

        return activities.get(txId);
    }

    public List<Activity> findAll() {
        return activities.values().stream().collect(Collectors.toList());
    }

    public void add(Activity activity) {
        activities.putIfAbsent(activity.id, activity);
    }

    public void remove(String id) {
        activities.remove(id);
    }
}
