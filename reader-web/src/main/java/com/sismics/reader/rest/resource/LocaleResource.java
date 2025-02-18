```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.LocaleDao;
import com.sismics.reader.core.model.jpa.Locale;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Locale REST resources.
 *
 * @author jtremeaux
 */
@Path("/locale")
public class LocaleResource extends BaseResource {

    /**
     * Returns all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllLocales() throws JSONException {
        List<JSONObject> items = new ArrayList<>();

        for (Locale locale : new LocaleDao().findAll()) {
            JSONObject item = new JSONObject();
            item.put("id", locale.getId());
            items.add(item);
        }

        JSONObject response = new JSONObject();
        response.put("locales", items);
        return Response.ok().entity(response).build();
    }
}
```