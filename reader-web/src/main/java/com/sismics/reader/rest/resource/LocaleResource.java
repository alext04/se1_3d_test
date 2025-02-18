```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.service.LocaleService;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.inject.Inject;
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

    @Inject
    private LocaleService localeService;

    /**
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLocalesAsJson() throws JSONException {
        JSONObject response = localeService.getLocalesAsJson();
        return Response.ok().entity(response).build();
    }
}
```