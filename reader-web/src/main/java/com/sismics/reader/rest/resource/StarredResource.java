Refactored Code:

```java
import com.sismics.reader.core.dao.jpa.StarredArticleDao;
import com.sismics.reader.core.model.jpa.StarredArticle;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Starred articles REST resources.
 *
 * @author jtremeaux
 */
@Path("/starred")
public class StarredResource extends BaseResource {

    private final StarredArticleDao starredArticleDao;

    public StarredResource(StarredArticleDao starredArticleDao) {
        this.starredArticleDao = starredArticleDao;
    }

    /**
     * Returns starred articles.
     *
     * @param limit Page limit
     * @param afterArticle Start the list after this article
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @QueryParam("limit") Integer limit,
            @QueryParam("after_article") String afterArticle) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        List<StarredArticle> starredArticles = starredArticleDao.findByUserId(principal.getId(), limit, afterArticle);

        List<JSONObject> articles = ArticleAssembler.asJson(starredArticles);
        JSONObject response = new JSONObject();
        response.put("articles", articles);

        return Response.ok().entity(response).build();
    }

    /**
     * Marks an article as starred.
     *
     * @param id User article ID
     * @return Response
     */
    @PUT
    @Path("/{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response star(@PathParam("id") String id) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        StarredArticle starredArticle = starredArticleDao.star(id, principal.getId());

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    /**
     * Marks an article as unstarred.
     *
     * @param id User article ID
     * @return Response
     */
    @DELETE
    @Path("/{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response unstar(@PathParam("id") String id) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        starredArticleDao.unstar(id, principal.getId());

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }
}
```