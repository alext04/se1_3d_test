**Refactored Code (Java):**

```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.util.ConfigUtil;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.reader.rest.dto.LogCriteriaDTO;
import com.sismics.reader.rest.dto.LogDTO;
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
import java.util.stream.Collectors;

/**
 * General app REST resource.
 *
 * @author jtremeaux
 */
@Path("/app")
public class AppResource extends BaseResource {

    /**
     * Return the information about the application.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response version() throws JSONException {
        VersionData versionData = getVersionData();
        return Response.ok().entity(versionData).build();
    }

    private VersionData getVersionData() {
        ResourceBundle configBundle = ConfigUtil.getConfigBundle();
        String currentVersion = configBundle.getString("api.current_version");
        String minVersion = configBundle.getString("api.min_version");

        return new VersionData(currentVersion, minVersion);
    }

    /**
     * Retrieve the application logs.
     *
     * @param logCriteriaDTO Filter criteria for logs
     * @return Response
     */
    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLogs(LogCriteriaDTO logCriteriaDTO) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        LogCriteria logCriteria = logCriteriaDTO.toLogCriteria();

        MemoryAppender memoryAppender = getMemoryAppender();

        PaginatedList<LogEntry> paginatedList = PaginatedLists.create(logCriteriaDTO.getLimit(), logCriteriaDTO.getOffset());
        memoryAppender.find(logCriteria, paginatedList);

        return Response.ok().entity(getLogsResponse(paginatedList)).build();
    }

    private MemoryAppender getMemoryAppender() {
        Logger logger = Logger.getRootLogger();
        Appender appender = logger.getAppender("MEMORY");
        if (appender == null || !(appender instanceof MemoryAppender)) {
            throw new ServerException("ServerError", "MEMORY appender not configured");
        }
        return (MemoryAppender) appender;
    }

    private JSONObject getLogsResponse(PaginatedList<LogEntry> paginatedList) throws JSONException {
        JSONObject response = new JSONObject();
        List<LogDTO> logs = paginatedList.getResultList().stream()
                .map(this::toLogDTO)
                .collect(Collectors.toList());
        response.put("total", paginatedList.getResultCount());
        response.put("logs", logs);
        return response;
    }

    private LogDTO toLogDTO(LogEntry logEntry) {
        return new LogDTO(logEntry.getTimestamp(), logEntry.getLevel(), logEntry.getTag(), logEntry.getMessage());
    }

    /**
     * Destroy and rebuild articles index.
     *
     * @return Response
     */
    @POST
    @Path("batch/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response batchReindex() throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        try {
            AppContext.getInstance().getIndexingService().rebuildIndex();
        } catch (Exception e) {
            throw new ServerException("IndexingError", "Error rebuilding index", e);
        }

        return Response.ok().entity(new StatusResponse("ok")).build();
    }

    /**
     * Attempt to map a port to the gateway.
     *
     * @return Response
     */
    @POST
    @Path("map_port")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mapPort() throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        if (!NetworkUtil.mapTcpPort(request.getServerPort())) {
            throw new ServerException("NetworkError", "Error mapping port using UPnP");
        }

        return Response.ok().entity(new StatusResponse("ok")).build();
    }
}

class VersionData {
    private String currentVersion;
    private String minVersion;

    public VersionData(String currentVersion, String minVersion) {
        this.currentVersion = currentVersion;
        this.minVersion = minVersion;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getMinVersion() {
        return minVersion;
    }
}

class StatusResponse {
    private String status;

    public StatusResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
```