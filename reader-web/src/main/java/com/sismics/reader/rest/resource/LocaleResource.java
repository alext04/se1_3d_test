```java
package com.sismics.reader.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sismics.reader.core.dao.LocaleDao;
import com.sismics.reader.core.model.Locale;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Locale REST resources.
 *
 * @author jtremeaux
 */
@Path("/locale")
public class LocaleResource extends BaseResource {
    private final LocaleDao localeDao;
    private final ObjectMapper objectMapper;

    public LocaleResource(LocaleDao localeDao, ObjectMapper objectMapper) {
        this.localeDao = localeDao;
        this.objectMapper = objectMapper;
    }

    /**
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        return Response.ok(objectMapper.writeValueAsString(localeDao.findAll())).build();
    }
}
```