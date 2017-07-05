package participant.filter.service;

import participant.filter.model.Booking;
import participant.filter.model.BookingStatus;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BookingStore {
    private Map<String, Booking> bookings = new HashMap<>();

    public Booking get(String bookingId) throws NotFoundException {
        if (!bookings.containsKey(bookingId))
            throw new NotFoundException(Response.status(404).entity("Invalid bookingId id: " + bookingId).build());

        return bookings.get(bookingId);
    }

    public List<Booking> findAll() {
        return bookings.values().stream().collect(Collectors.toList());
    }

    public Booking add(Booking booking) {
        return bookings.putIfAbsent(booking.getId(), booking);
    }

    public Booking remove(String id) {
        return bookings.remove(id);
    }

    public void updateBookingStatus(String bookingId, BookingStatus status) {
        get(bookingId).setStatus(status);
    }
}
