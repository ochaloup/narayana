package participant.api;

import javax.ws.rs.core.Response;

public class BookingException extends Exception {
    int reason;

    public int getReason() {
        return reason;
    }

    public BookingException(int reason, String message) {
        super(message);

        this.reason = reason;

    }
}
