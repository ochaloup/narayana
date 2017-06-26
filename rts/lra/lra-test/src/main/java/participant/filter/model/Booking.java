package participant.filter.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Booking implements Serializable {
    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("quantity") private Integer quantity;

    @JsonCreator
    public Booking(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("quantity")Integer quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public Booking(Booking booking1, Booking booking2) {
        this.id = booking1.id;
        this.name = String.format("%s,%s", booking1.getName(), booking2.getName());
        this.quantity = Math.max(booking1.getQuantity(), booking2.getQuantity());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
