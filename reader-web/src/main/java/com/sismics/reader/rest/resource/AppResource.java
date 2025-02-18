```java
package com.sismics.reader.rest.resource;

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
import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

@Path("/app")
public class AppResource extends BaseResource {
    private static final String MEMORY_APPENDER_NAME = "MEMORY";
    private static final String STATUS_OK = "ok";
    private static final String JSON_STATUS = "status";
    private static final String JSON_TOTAL = "total";
    private static final String JSON_LOGS = "logs";
    private static final String JSON_CURRENT_VERSION = "current_version";
    private static final String JSON_MIN_VERSION = "min_version";
    private static final String JSON_TOTAL_MEMORY = "total_memory";
    private static final String JSON_FREE_MEMORY = "free_memory";

    private void checkAdminAccess() {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
    }

    private JSONObject createSuccessResponse() throws JSONException {
        JSONObject response = new JSONObject();
        response.put(JSON_STATUS, STATUS_OK);
        return response;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAppInfo() throws JSONException {
        ResourceBundle configBundle = ConfigUtil.getConfigBundle();
        String currentVersion = configBundle.getString("api.current_version");
        String minVersion = configBundle.getString("api.min_version");

        JSONObject response = new JSONObject();
        response.put(JSON_CURRENT_VERSION, currentVersion.replace("-SNAPSHOT", ""));
        response.put(JSON_MIN_VERSION, minVersion);
        response.put(JSON_TOTAL_MEMORY, Runtime.getRuntime().totalMemory());
        response.put(JSON_FREE_MEMORY, Runtime.getRuntime().freeMemory());
        return Response.ok().entity(response).build();
    }

    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public Response log(
            @QueryParam("level") String level,
            @QueryParam("tag") String tag,
            @QueryParam("message") String message,
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset) throws JSONException {
        checkAdminAccess();
        MemoryAppender memoryAppender = getMemoryAppender();
        LogCriteria logCriteria = buildLogCriteria(level, tag, message);
        PaginatedList<LogEntry> paginatedList = PaginatedLists.create(limit, offset);
        memoryAppender.find(logCriteria, paginatedList);
        return Response.ok().entity(buildLogResponse(paginatedList)).build();
    }

    private MemoryAppender getMemoryAppender() {
        Logger logger = Logger.getRootLogger();
        Appender appender = logger.getAppender(MEMORY_APPENDER_NAME);
        if (!(appender instanceof MemoryAppender)) {
            throw new ServerException("MemoryAppenderError", "MEMORY appender not configured");
        }
        return (MemoryAppender) appender;
    }

    private LogCriteria buildLogCriteria(String level, String tag, String message) {
        return new LogCriteria()
            .setLevel(StringUtils.stripToNull(level))
            .setTag(StringUtils.stripToNull(tag))
            .setMessage(StringUtils.stripToNull(message));
    }

    private JSONObject buildLogResponse(PaginatedList<LogEntry> paginatedList) throws JSONException {
        JSONObject response = new JSONObject();
        response.put(JSON_TOTAL, paginatedList.getResultCount());
        response.put(JSON_LOGS, convertLogEntriesToJson(paginatedList.getResultList()));
        return response;
    }

    private List<JSONObject> convertLogEntriesToJson(List<LogEntry> logEntries) throws JSONException {
        List<JSONObject> logs = new ArrayList<>();
        for (LogEntry entry : logEntries) {
            logs.add(convertLogEntryToJson(entry));
        }
        return logs;
    }

    private JSONObject convertLogEntryToJson(LogEntry entry) throws JSONException {
        JSONObject log = new JSONObject();
        log.put("date", entry.getTimestamp());
        log.put("level", entry.getLevel());
        log.put("tag", entry.getTag());
        log.put("message", entry.getMessage());
        return log;
    }

    @POST
    @Path("batch/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response batchReindex() throws JSONException {
        checkAdminAccess();
        try {
            AppContext.getInstance().getIndexingService().rebuildIndex();
        } catch (Exception e) {
            throw new ServerException("IndexingError", "Error rebuilding index", e);
        }
        return Response.ok().entity(createSuccessResponse()).build();
    }

    @POST
    @Path("map_port")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mapPort() throws JSONException {
        checkAdminAccess();
        if (!NetworkUtil.mapTcpPort(request.getServerPort())) {
            throw new ServerException("PortMappingError", "Error mapping port using UPnP");
        }
        return Response.ok().entity(createSuccessResponse()).build();
    }
}
```