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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Locale REST resources.
 *
 * @author jtremeaux
 */
@Path("/locale")
public class LocaleResource extends BaseResource {

    private LocaleDao localeDao = new LocaleDao();

    /**
     * Returns the list of all locales.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws JSONException {
        List<Locale> localeList = localeDao.findAll();

        List<JSONObject> items = localeList.stream()
                .map(locale -> createLocaleJson(locale))
                .collect(Collectors.toList());

        JSONObject response = new JSONObject();
        response.put("locales", items);

        return Response.ok().entity(response).build();
    }

    private JSONObject createLocaleJson(Locale locale) throws JSONException {
        JSONObject item = new JSONObject();
        item.put("id", locale.getId());

        return item;
    }
}