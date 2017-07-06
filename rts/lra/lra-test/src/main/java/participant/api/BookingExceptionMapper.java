package participant.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class BookingExceptionMapper implements ExceptionMapper<BookingException> {
    @Override
    public Response toResponse(BookingException exception) {

        return Response.status(exception.getReason())
                .entity(exception.getMessage()).build();
    }
}
