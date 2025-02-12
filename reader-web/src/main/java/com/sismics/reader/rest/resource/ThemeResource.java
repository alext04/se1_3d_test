```java
import com.sismics.reader.rest.dao.ThemeDao;
import com.sismics.rest.exception.ServerException;
import com.sismics.util.EnvironmentUtil;
import org.codehaus.jettison.json.JSONArray;
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
 * Theme REST resources.
 * 
 * @author jtremeaux
 */
@Path("/theme")
public class ThemeResource extends BaseResource {
    /**
     * Returns the list of all themes.
     * 
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        ThemeDao themeDao = new ThemeDao();
        List<String> themeList;
        try {
            themeList = themeDao.findAll(EnvironmentUtil.isUnitTest() ? null : request.getServletContext());
        } catch (Exception e) {
            throw new ServerException("UnknownError", "Error getting theme list", e);
        }
        JSONObject response = new JSONObject();
        JSONArray items = new JSONArray();
        for (String theme : themeList) {
            try {
                JSONObject item = new JSONObject();
                item.put("id", theme);
                items.put(item);
            } catch (JSONException e) {
                // Ignore and continue
            }
        }
        try {
            response.put("themes", items);
        } catch (JSONException e) {
            // Ignore and return empty response
        }
        return Response.ok().entity(response).build();
    }
}
```