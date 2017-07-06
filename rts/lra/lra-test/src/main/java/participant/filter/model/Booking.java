package participant.filter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class Booking {
    private AtomicLong nextBid = new AtomicLong(0);

    @JsonProperty("id") private String id;
    @JsonProperty("name") private String name;
    @JsonProperty("quantity") private Integer quantity;
    @JsonProperty("status") private BookingStatus status;
    @JsonProperty("type") private String type;
    @JsonProperty("details") private Booking[] details;

    public Booking(String id, String name, Integer quantity, String type) {
        this(id, name, quantity, type, BookingStatus.PROVISIONAL, null);
    }

    public Booking(String id, String type, Booking... bookings) {
        this(id, "Aggregate Booking", 1, type, BookingStatus.PROVISIONAL, bookings);
    }

    @JsonCreator
    public Booking(@JsonProperty("id") String id,
                   @JsonProperty("name") String name,
                   @JsonProperty("quantity") Integer quantity,
                   @JsonProperty("type") String type,
                   @JsonProperty("status") BookingStatus status,
                   @JsonProperty("details") Booking[] details) {

        init(id, name, quantity, type, status, details);
    }

/*    public Booking(String id, String type, Booking... bookings) {
        detailSb.append(bookings.length).append(RECORD_SEPARATOR);

        // TOTO use a json string instead
        Arrays.stream(bookings).forEach(b -> {
            if (b != null)
                detailSb
                        .append(b.getId()).append(RECORD_SEPARATOR)
                        .append(b.getName()).append(RECORD_SEPARATOR)
                        .append(b.getQuantity()).append(RECORD_SEPARATOR);
        });*/



    private void init(String id, String name, Integer quantity, String type, BookingStatus status, Booking[] details) {
        this.id = id == null ? Long.valueOf(nextBid.incrementAndGet()).toString() : id;
        this.name = name == null ? "" : name;
        this.quantity = quantity;
        this.type = type == null ? "" : type;
        this.status = status;
        this.details = details == null ? new Booking[0] : removeNullEnElements(details);
    }

    @SuppressWarnings("unchecked")
    private <T> T[] removeNullEnElements(T[] a) {
        List<T> list = new ArrayList<T>(Arrays.asList(a));
        list.removeAll(Collections.singleton(null));
        return list.toArray((T[]) Array.newInstance(a.getClass().getComponentType(), list.size()));
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

    public String getType() {
        return type;
    }

    public Booking[] getDetails() {
        return details;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void cancel() {
        status = BookingStatus.CANCEL_REQUESTED;
    }

    public void confirm() {
        status = BookingStatus.CONFIRMED;
    }

    public String toString() {
        return String.format("{\"id\":\"%s\",\"name\":\"%s\",\"quantity\":\"%d\",\"type\":\"%s\",\"status\":\"%s\"}",
                    id, name, quantity, type, status);

        // js.append('[').append(Arrays.stream(details).map(Booking::toString).collect(Collectors.joining(","))).append(']');

/*        if (json == null) {
            StringBuilder js = new StringBuilder(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"quantity\":\"%d\",\"type\":\"%s\",\"status\":\"%s\"}",
                    id, name, quantity, type, status));

            // append the array of bookings as ,[{...},{...}]

            js.append('[').append(Arrays.stream(details).map(Booking::toString).collect(Collectors.joining(","))).append(']');

            json = js.toString();
        }

        return json;*/
    }

    public void canceled() {
        status = BookingStatus.CANCELLED;
    }

    public void confirmirming() {
        status = BookingStatus.CONFIRMING;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public boolean merge(Booking booking) {
        if (!id.equals(booking.getId()))
            return false; // or throw an exception

        name = booking.getName();
        quantity = booking.getQuantity();
        status = booking.getStatus();

        return true;
    }

    @JsonIgnore
    public String getEncodedId() {
        try {
            return URLEncoder.encode(id, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return id; // TODD do it in the constructor
        }
    }
}
