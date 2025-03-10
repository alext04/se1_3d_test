```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.LocaleDao;
import com.sismics.reader.core.dto.LocaleDto;
import com.sismics.reader.core.model.jpa.Locale;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
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

    @Inject
    private LocaleDao localeDao;

    /**
     * Returns the list of all locales.
     * 
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() {
        List<Locale> localeList = localeDao.findAll();

        List<LocaleDto> localeDtos = localeList.stream()
                .map(locale -> new LocaleDto(locale.getId()))
                .collect(Collectors.toList());

        return buildJsonResponse(localeDtos);
    }


    private Response buildJsonResponse(List<LocaleDto> locales) {
        JsonArrayBuilder localesArrayBuilder = Json.createArrayBuilder();
        for (LocaleDto locale : locales) {
            JsonObjectBuilder localeObjectBuilder = Json.createObjectBuilder();
            localeObjectBuilder.add("id", locale.getId());
            localesArrayBuilder.add(localeObjectBuilder);
        }

        JsonObjectBuilder responseBuilder = Json.createObjectBuilder();
        responseBuilder.add("locales", localesArrayBuilder);

        return Response.ok().entity(responseBuilder.build()).build();
    }
}


// New DTO class
package com.sismics.reader.core.dto;

public class LocaleDto {
    private String id;

    public LocaleDto(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
```