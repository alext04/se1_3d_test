**Refactored Code:**

```java
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.util.ConfigUtil;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.util.NetworkUtil;
import com.sismics.util.log4j.LogCriteria;
import com.sismics.util.log4j.LogEntry;
import com.sismics.util.log4j.MemoryAppender;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * General app REST resource.
 *
 * @author jtremeaux
 */
@Path("/app")
public class AppResource extends BaseResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response version() throws JSONException {
        ResourceBundle configBundle = ConfigUtil.getConfigBundle();
        String currentVersion = configBundle.getString("api.current_version");
        String minVersion = configBundle.getString("api.min_version");

        JSONObject response = new JSONObject();
        response.put("current_version", currentVersion.replace("-SNAPSHOT", ""));
        response.put("min_version", minVersion);
        response.put("total_memory", Runtime.getRuntime().totalMemory());
        response.put("free_memory", Runtime.getRuntime().freeMemory());
        return Response.ok().entity(response).build();
    }

    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogs(
            @QueryParam("level") String level,
            @QueryParam("tag") String tag,
            @QueryParam("message") String message,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset) throws JSONException {

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // Find the logs
        LogCriteria logCriteria = new LogCriteria()
                .setLevel(StringUtils.stripToNull(level))
                .setTag(StringUtils.stripToNull(tag))
                .setMessage(StringUtils.stripToNull(message));

        PaginatedList<LogEntry> paginatedList = PaginatedLists.create(limit, offset);
        getMemoryAppender().find(logCriteria, paginatedList);
        JSONObject response = new JSONObject();
        List<JSONObject> logs = new ArrayList<>();
        for (LogEntry logEntry : paginatedList.getResultList()) {
            JSONObject log = new JSONObject();
            log.put("date", logEntry.getTimestamp());
            log.put("level", logEntry.getLevel());
            log.put("tag", logEntry.getTag());
            log.put("message", logEntry.getMessage());
            logs.add(log);
        }
        response.put("total", paginatedList.getResultCount());
        response.put("logs", logs);

        return Response.ok().entity(response).build();
    }

    @POST
    @Path("batch/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reindex() throws JSONException {

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        JSONObject response = new JSONObject();
        try {
            AppContext.getInstance().getIndexingService().rebuildIndex();
        } catch (Exception e) {
            throw new ServerException("IndexingError", "Error rebuilding index", e);
        }
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    @POST
    @Path("map_port")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mapPort() throws JSONException {

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        JSONObject response = new JSONObject();
        if (!NetworkUtil.mapTcpPort(request.getServerPort())) {
            throw new ServerException("NetworkError", "Error mapping port using UPnP");
        }

        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    private MemoryAppender getMemoryAppender() {
        Logger logger = Logger.getRootLogger();
        Appender appender = logger.getAppender("MEMORY");
        if (appender == null || !(appender instanceof MemoryAppender)) {
            throw new ServerException("ServerError", "MEMORY appender not configured");
        }
        return (MemoryAppender) appender;
    }
}
```