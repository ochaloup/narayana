package participant.filter.service;

import org.jboss.narayana.rts.lra.coordinator.api.LRAClient;
import participant.filter.model.Booking;
import participant.filter.model.BookingStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class TripService extends BookingStore{
    @Inject
    private LRAClient lraClient;

    public Booking confirmBooking(Booking booking) {
        System.out.printf("Confirming booking id %s (%s) status: %s%n",
                booking.getId(), booking.getName(), booking.getStatus());

        if (booking.getStatus() == BookingStatus.CANCEL_REQUESTED)
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST)
                    .entity("Trying to confirm a booking which needs to be cancelled")
                    .build());

        Booking prev = add(booking);

        if (prev != null)
            System.out.printf("Seen this booking before%n");

        Arrays.stream(booking.getDetails()).forEach(b -> {
            System.out.printf("\tid %s (%s) status: %s%n",
                    b.getId(), b.getName(), b.getStatus());

            if (b.getStatus().equals(BookingStatus.CANCEL_REQUESTED)) {
                lraClient.cancelLRA(LRAClient.lraToURL(b.getId(), "Invalid " + b.getType() + " booking id format"));
                b.canceled();
            }
        });

        booking.confirmirming();

        lraClient.closeLRA(booking.getId());

        booking.confirm();

        return booking;
    }

    public Booking cancelBooking(Booking booking) {
        System.out.printf("Canceling booking id %s (%s) status: %s%n",
                booking.getId(), booking.getName(), booking.getStatus());

        if (booking.getStatus() != BookingStatus.CANCEL_REQUESTED && booking.getStatus() != BookingStatus.PROVISIONAL)
            throw new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).entity("To late to cancel booking").build());

        Booking prev = add(booking);

        if (prev != null)
            System.out.printf("Seen this booking before%n");

        booking.cancel();

        lraClient.cancelLRA(LRAClient.lraToURL(booking.getId(), "Invalid trip booking id format"));

        booking.canceled();

        return booking;
    }
}
