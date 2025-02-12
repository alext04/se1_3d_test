**Refactored Code:**

```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.core.dao.jpa.FeedSubscriptionDao;
import com.sismics.reader.core.dao.jpa.UserArticleDao;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * All articles REST resources.
 *
 * @author jtremeaux
 */
@Path("/articles")
public class ArticlesResource extends BaseResource {

    private static final String PATH = "/articles";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @QueryParam("unread") boolean unread,
            @QueryParam("limit") Integer limit,
            @QueryParam("after_article") String afterArticle) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        Map<String, Object> queryParameters = new HashMap<>();
        queryParameters.put("unread", unread);
        queryParameters.put("limit", limit);
        queryParameters.put("after_article", afterArticle);
        queryParameters.put("userId", principal.getId());

        PaginatedList<UserArticleDto> paginatedList = PaginatedLists.create(limit, null);
        UserArticleDao userArticleDao = new UserArticleDao();
        UserArticleCriteria userArticleCriteria = new UserArticleCriteria();
        List<UserArticleDto> articles = userArticleDao.findArticlesForCurrentUser(paginatedList, userArticleCriteria, queryParameters);

        List<JSONObject> articlesJson = articles.stream()
                .map(ArticleAssembler::asJson)
                .collect(Collectors.toList());

        JSONObject response = new JSONObject();
        response.put("articles", articlesJson);
        return Response.ok().entity(response).build();
    }

    @POST
    @Path("/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read() throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        UserArticleDao userArticleDao = new UserArticleDao();
        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();

        userArticleDao.markAllAsRead(principal.getId());
        feedSubscriptionDao.updateAllUnreadCounts(principal.getId());

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }
}
```