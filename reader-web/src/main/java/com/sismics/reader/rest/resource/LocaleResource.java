```java
import com.sismics.reader.core.dao.jpa.LocaleDao;
import com.sismics.reader.core.model.jpa.Locale;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

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
    /**
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws JSONException {
        LocaleDao localeDao = new LocaleDao();
        List<Locale> localeList = localeDao.findAll();
        return Response.ok().entity(toJSONArray(localeList)).build();
    }

    private JSONObject toJSONArray(List<Locale> locales) throws JSONException {
        JSONObject response = new JSONObject();
        List<JSONObject> items = locales.stream()
                .map(this::toJSON)
                .collect(Collectors.toList());
        response.put("locales", items);
        return response;
    }

    private JSONObject toJSON(Locale locale) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("localeId", locale.getId());
        return item;
    }
}
```