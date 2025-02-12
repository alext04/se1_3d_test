```java
import com.sismics.reader.core.dao.jpa.CategoryDao;
import com.sismics.reader.core.dao.jpa.FeedSubscriptionDao;
import com.sismics.reader.core.dao.jpa.UserArticleDao;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserArticleCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.model.jpa.FeedSubscription;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.util.ValidationUtil;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.persistence.NoResultException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.*;

/**
 * Category REST resources.
 */
@Path("/category")
public class CategoryResource extends BaseResource {

    private static final int PAGE_SIZE_DEFAULT = 20;

    /**
     * List categories.
     *
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list() throws JSONException {
        checkAuthentication();

        CategoryDao categoryDao = new CategoryDao();
        Category rootCategory = categoryDao.getRootCategory(principal.getId());

        List<Category> categories = List.of(rootCategory);

        List<JSONObject> jsonCategories = new ArrayList<>();
        for (Category category : categories) {
            JSONObject jsonCategory = new JSONObject();
            jsonCategory.put("id", category.getId());

            List<Category> children = categoryDao.findSubCategory(category.getId(), principal.getId());
            if (!children.isEmpty()) {
                JSONArray jsonChildren = new JSONArray();
                for (Category child : children) {
                    JSONObject jsonChild = new JSONObject();
                    jsonChild.put("id", child.getId());
                    jsonChild.put("name", child.getName());

                    jsonChildren.put(jsonChild);
                }
                jsonCategory.put("categories", jsonChildren);
            }

            jsonCategories.add(jsonCategory);
        }

        JSONObject response = new JSONObject();
        response.put("categories", jsonCategories);
        return Response.ok().entity(response).build();
    }

    /**
     * List articles in a category.
     *
     * @param id         Category ID
     * @param unread      Returns only unread articles
     * @param limit       Page limit
     * @param afterArticle Start the list after this article
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getArticles(
            @PathParam("id") String id,
            @QueryParam("unread") boolean unread,
            @QueryParam("limit") Integer limit,
            @QueryParam("after_article") String afterArticle) throws JSONException {
        checkAuthentication();

        UserArticleCriteria criteria = new UserArticleCriteria()
                .setUnread(unread)
                .setUserId(principal.getId())
                .setSubscribed(true)
                .setVisible(true);

        if (!id.equals("root")) {
            criteria.setCategoryId(id);
        }

        if (afterArticle != null) {
            UserArticleCriteria afterArticleCriteria = new UserArticleCriteria()
                    .setUserArticleId(afterArticle)
                    .setUserId(principal.getId());
            List<UserArticleDto> articles = new UserArticleDao().findByCriteria(afterArticleCriteria);
            if (articles.isEmpty()) {
                throw new ClientException("ArticleNotFound", MessageFormat.format("Can't find user article {0}", afterArticle));
            }

            UserArticleDto article = articles.get(0);
            criteria.setArticlePublicationDateMax(new Date(article.getArticlePublicationTimestamp()));
            criteria.setArticleIdMax(article.getArticleId());
        }

        int pageSize = limit != null ? limit : PAGE_SIZE_DEFAULT;
        PaginatedList<UserArticleDto> articles = PaginatedLists.create(pageSize, null);
        new UserArticleDao().findByCriteria(articles, criteria, null, null);

        JSONObject response = new JSONObject();
        response.put("articles", ArticleAssembler.asJson(articles.getResultList()));

        return Response.ok().entity(response).build();
    }

    /**
     * Create a category.
     *
     * @param name Category name
     * @return Response
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(@FormParam("name") String name) throws JSONException {
        checkAuthentication();

        validateName(name);

        CategoryDao categoryDao = new CategoryDao();
        Category rootCategory = categoryDao.getRootCategory(principal.getId());
        int displayOrder = categoryDao.getCategoryCount(rootCategory.getId(), principal.getId());

        Category category = new Category();
        category.setUserId(principal.getId());
        category.setParentId(rootCategory.getId());
        category.setName(name);
        category.setOrder(displayOrder);

        JSONObject response = new JSONObject();
        response.put("id", categoryDao.create(category));
        return Response.ok().entity(response).build();
    }

    /**
     * Delete a category.
     *
     * @param id Category ID
     * @return Response
     */
    @DELETE
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response delete(@PathParam("id") String id) throws JSONException {
        checkAuthentication();

        getCategory(id);

        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        List<FeedSubscription> subscriptions = feedSubscriptionDao.findByCategory(id);
        Category rootCategory = new CategoryDao().getRootCategory(principal.getId());
        for (FeedSubscription subscription : subscriptions) {
            subscription.setCategoryId(rootCategory.getId());
            feedSubscriptionDao.update(subscription);
            feedSubscriptionDao.reorder(subscription, 0);
        }

        new CategoryDao().delete(id);

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    /**
     * Mark all articles in this category as read.
     *
     * @param id Category ID
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}/read")
    @Produces(MediaType.APPLICATION_JSON)
    public Response read(@PathParam("id") String id) throws JSONException {
        checkAuthentication();

        getCategory(id);

        new UserArticleDao().markAsRead(new UserArticleCriteria()
                .setUserId(principal.getId())
                .setSubscribed(true)
                .setCategoryId(id));

        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        for (FeedSubscriptionDto subscription : feedSubscriptionDao.findByCriteria(new FeedSubscriptionCriteria()
                .setCategoryId(id)
                .setUserId(principal.getId()))) {
            feedSubscriptionDao.updateUnreadCount(subscription.getId(), 0);
        }

        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    /**
     * Update a category.
     *
     * @param id       Category ID
     * @param name     Category name
     * @param order    Display order of this category
     * @param folded   True if this category is folded in the subscriptions tree.
     * @return Response
     */
    @POST
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @PathParam("id") String id,
            @FormParam("name") String name,
            @FormParam("order") Integer order,
            @FormParam("folded") Boolean folded) throws JSONException {
        checkAuthentication();

        Category category = getCategory(id);

        if (