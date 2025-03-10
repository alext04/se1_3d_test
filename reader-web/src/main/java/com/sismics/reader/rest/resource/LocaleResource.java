```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.LocaleDao;
import com.sismics.reader.core.dto.LocaleDto;
import com.sismics.reader.rest.exception.ReaderException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Locale REST resources.
 *
 * @author jtremeaux
 */
@Path("/locale")
public class LocaleResource extends BaseResource {

    private final LocaleDao localeDao;

    @Inject
    public LocaleResource(LocaleDao localeDao) {
        this.localeDao = localeDao;
    }

    /**
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        try {
            List<LocaleDto> localeDtos = localeDao.findAll().stream()
                    .map(locale -> new LocaleDto(locale.getId()))
                    .collect(Collectors.toList());

            JSONArray locales = new JSONArray(localeDtos);
            JSONObject response = new JSONObject();
            response.put("locales", locales);

            return Response.ok().entity(response).build();
        } catch (Exception e) {
            throw new ReaderException("Error retrieving locales", e);
        }
    }
}


```

```java
package com.sismics.reader.core.dto;

public class LocaleDto {
    private String id;

    public LocaleDto(String id) {
        this.id = id;
    }

    // Getters and setters (or use Lombok's @Data)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
```

```java
package com.sismics.reader.rest.exception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

public class ReaderException extends WebApplicationException {
    public ReaderException(String message, Throwable cause) {
        super(message, cause, Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorMessage(message)) // Assuming you have an ErrorMessage class
                .build());
    }
}

class ErrorMessage { // Example ErrorMessage class. Adapt as needed
    private String message;
    public ErrorMessage(String message) { this.message = message; }
    public String getMessage() { return message; }
}
```
