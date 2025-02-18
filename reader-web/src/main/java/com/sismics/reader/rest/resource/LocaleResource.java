**Refactored Code:**

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

/**
 * Locale REST resources.
 *
 * @author jtremeaux
 */
@Path("/locale")
public class LocaleResource extends BaseResource {

    private static final String LOCALE_ATTR_ID = "id";

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

        JSONObject response = new JSONObject();
        response.put("locales", createLocaleItems(localeList));
        return Response.ok().entity(response).build();
    }

    /**
     * Creates a list of JSON objects representing the locales.
     *
     * @param localeList the list of locales
     * @return the list of JSON objects
     */
    private List<JSONObject> createLocaleItems(List<Locale> localeList) throws JSONException {
        List<JSONObject> items = new ArrayList<>();
        for (Locale locale : localeList) {
            items.add(createLocaleItem(locale));
        }
        return items;
    }

    /**
     * Creates a JSON object representing a locale.
     *
     * @param locale the locale
     * @return the JSON object
     */
    private JSONObject createLocaleItem(Locale locale) throws JSONException {
        JSONObject item = new JSONObject();
        item.put(LOCALE_ATTR_ID, locale.getId());
        return item;
    }
}
```