```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.LocaleRepository;
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
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws JSONException {
        LocaleRepository localeRepository = new LocaleRepository();
        List<Locale> localeList = localeRepository.findAll();
        List<JSONObject> items = createLocaleItems(localeList);
        JSONObject response = new JSONObject();
        response.put("locales", items);
        return Response.ok().entity(response).build();
    }

    private List<JSONObject> createLocaleItems(List<Locale> localeList) throws JSONException {
        List<JSONObject> items = new ArrayList<JSONObject>();
        for (Locale locale : localeList) {
            JSONObject item = new JSONObject();
            item.put("id", locale.getId());
            items.add(item);
        }
        return items;
    }
}
```