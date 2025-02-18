```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.service.LocaleService;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
    private final LocaleService localeService;

    public LocaleResource(LocaleService localeService) {
        this.localeService = localeService;
    }

    /**
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws JSONException {
        JSONArray items = new JSONArray();
        for (JSONObject locale : localeService.findAll()) {
            items.put(locale);
        }
        JSONObject response = new JSONObject();
        response.put("locales", items);
        return Response.ok().entity(response).build();
    }
}
```