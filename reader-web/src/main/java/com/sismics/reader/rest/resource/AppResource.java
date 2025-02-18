**Refactored Code:**

```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.exception.ForbiddenClientException;
import com.sismics.reader.core.exception.ServerException;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.reader.rest.dto.LogEntryDTO;
import com.sismics.reader.rest.dto.LogResponseDTO;
import com.sismics.reader.rest.dto.VersionResponseDTO;
import com.sismics.reader.rest.util.LogCriteriaBuilder;
import com.sismics.reader.rest.util.PaginatedListBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MemoryAppender;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Path("/app")
public class AppResource extends BaseResource {

    private static final Logger logger = Logger.getRootLogger();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response version() {
        VersionResponseDTO version = new VersionResponseDTO();
        version.setCurrentVersion(ConfigUtil.getConfigBundle().getString("api.current_version").replace("-SNAPSHOT", ""));
        version.setMinVersion(ConfigUtil.getConfigBundle().getString("api.min_version"));
        version.setTotalMemory(Runtime.getRuntime().totalMemory());
        version.setFreeMemory(Runtime.getRuntime().freeMemory());
        return Response.ok().entity(version).build();
    }

    @GET
    @Path("log")
    @Produces(MediaType.APPLICATION_JSON)
    public Response log(@QueryParam("level") String level, @QueryParam("tag") String tag, @QueryParam("message") String message,
                       @QueryParam("limit") Integer limit, @QueryParam("offset") Integer offset) {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        MemoryAppender memoryAppender = (MemoryAppender) logger.getAppender("MEMORY");
        if (memoryAppender == null) {
            throw new ServerException("ServerError", "MEMORY appender not configured");
        }

        LogCriteriaBuilder criteriaBuilder = new LogCriteriaBuilder()
                .withLevel(StringUtils.stripToNull(level))
                .withTag(StringUtils.stripToNull(tag))
                .withMessage(StringUtils.stripToNull(message));

        PaginatedListBuilder<LogEntry> paginatedList = new PaginatedListBuilder<LogEntry>(limit, offset);
        memoryAppender.find(criteriaBuilder.build(), paginatedList);

        List<LogEntryDTO> logs = paginatedList.getResultList().stream()
                .map(logEntry -> new LogEntryDTO(logEntry.getTimestamp(), logEntry.getLevel(), logEntry.getTag(), logEntry.getMessage()))
                .collect(Collectors.toList());

        LogResponseDTO response = new LogResponseDTO();
        response.setTotal(paginatedList.getResultCount());
        response.setLogs(logs);

        return Response.ok().entity(response).build();
    }

    @POST
    @Path("batch/reindex")
    @Produces(MediaType.APPLICATION_JSON)
    public Response batchReindex() {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        try {
            AppContext.getInstance().getIndexingService().rebuildIndex();
        } catch (Exception e) {
            throw new ServerException("IndexingError", "Error rebuilding index", e);
        }

        return Response.ok().entity(new ServerResponseDTO("ok")).build();
    }

    @POST
    @Path("map_port")
    @Produces(MediaType.APPLICATION_JSON)
    public Response mapPort() {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        if (!NetworkUtil.mapTcpPort(request.getServerPort())) {
            throw new ServerException("NetworkError", "Error mapping port using UPnP");
        }

        return Response.ok().entity(new ServerResponseDTO("ok")).build();
    }
}
```