```java
package com.sismics.reader.rest.resource;

import com.google.common.io.ByteStreams;
import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.*;
import com.sismics.reader.core.service.FeedService;
import com.sismics.reader.core.util.DirectoryUtil;
import com.sismics.reader.core.util.EntityManagerUtil;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.rest.assembler.ArticleAssembler;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.JsonUtil;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.util.MessageUtil;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataParam;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.persistence.NoResultException;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Feed subscriptions REST resources.
 *
 * @author jtremeaux
 */
@Path("/subscription")
public class SubscriptionResource extends BaseResource {

    /**
     * Returns the categories and subscriptions of the current user.
     *
     * @param unread Returns only subscriptions having unread articles
     * @return Response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response list(
            @QueryParam("unread") boolean unread) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Search this user's subscriptions
        FeedSubscriptionCriteria feedSubscriptionCriteria = new FeedSubscriptionCriteria()
                .setUserId(principal.getId())
                .setUnread(unread);

        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);

        // Get the root category
        CategoryDao categoryDao = new CategoryDao();
        Category rootCategory = categoryDao.getRootCategory(principal.getId());
        JSONObject rootCategoryJson = new JSONObject();
        rootCategoryJson.put("id", rootCategory.getId());

        // Construct the response
        List<JSONObject> rootCategories = new ArrayList<JSONObject>();
        rootCategories.add(rootCategoryJson);
        String oldCategoryId = null;
        JSONObject categoryJson = rootCategoryJson;
        int totalUnreadCount = 0;
        int categoryUnreadCount = 0;
        for (FeedSubscriptionDto feedSubscription : feedSubscriptionList) {
            String categoryId = feedSubscription.getCategoryId();
            String categoryParentId = feedSubscription.getCategoryParentId();

            if (!categoryId.equals(oldCategoryId)) {
                if (categoryParentId != null) {
                    if (categoryJson != rootCategoryJson) {
                        categoryJson.put("unread_count", categoryUnreadCount);
                        JsonUtil.append(rootCategoryJson, "categories", categoryJson);
                    }
                    categoryJson = new JSONObject();
                    categoryJson.put("id", categoryId);
                    categoryJson.put("name", feedSubscription.getCategoryName());
                    categoryJson.put("folded", feedSubscription.isCategoryFolded());
                    categoryJson.put("subscriptions", new JSONArray());
                    categoryUnreadCount = 0;
                }
            }
            JSONObject subscription = new JSONObject();
            subscription.put("id", feedSubscription.getId());
            subscription.put("title", feedSubscription.getFeedSubscriptionTitle());
            subscription.put("url", feedSubscription.getFeedRssUrl());
            subscription.put("unread_count", feedSubscription.getUnreadUserArticleCount());
            subscription.put("sync_fail_count", feedSubscription.getSynchronizationFailCount());
            JsonUtil.append(categoryJson, "subscriptions", subscription);

            oldCategoryId = categoryId;
            categoryUnreadCount += feedSubscription.getUnreadUserArticleCount();
            totalUnreadCount += feedSubscription.getUnreadUserArticleCount();
        }
        if (categoryJson != rootCategoryJson) {
            categoryJson.put("unread_count", categoryUnreadCount);
            JsonUtil.append(rootCategoryJson, "categories", categoryJson);
        }

        // Add the categories without subscriptions
        if (!unread) {
            List<Category> allCategoryList = categoryDao.findSubCategory(rootCategory.getId(), principal.getId());
            JSONArray categoryArrayJson = rootCategoryJson.optJSONArray("categories");
            List<JSONObject> fullCategoryListJson = new ArrayList<JSONObject>();
            int i = 0;
            for (Category category : allCategoryList) {
                if (categoryArrayJson != null && i < categoryArrayJson.length() && categoryArrayJson.getJSONObject(i).getString("id").equals(category.getId())) {
                    categoryJson = categoryArrayJson.getJSONObject(i++);
                } else {
                    categoryJson = new JSONObject();
                    categoryJson.put("id", category.getId());
                    categoryJson.put("name", category.getName());
                    categoryJson.put("folded", category.isFolded());
                    categoryJson.put("unread_count", 0);
                }
                fullCategoryListJson.add(categoryJson);
            }
            rootCategoryJson.put("categories", fullCategoryListJson);
        }

        JSONObject response = new JSONObject();
        response.put("categories", rootCategories);
        response.put("unread_count", totalUnreadCount);
        return Response.ok().entity(response).build();
    }

    /**
     * Returns the subscription informations and paginated articles.
     *
     * @param id Subscription ID
     * @param unread Returns only unread articles
     * @param limit Page limit
     * @param afterArticle Start the list after this article
     * @return Response
     */
    @GET
    @Path("{id: [a-z0-9\\-]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(
            @PathParam("id") String id,
            @QueryParam("unread") boolean unread,
            @QueryParam("limit") Integer limit,
            @QueryParam("after_article") String afterArticle) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the subscription
        FeedSubscriptionCriteria feedSubscriptionCriteria = new FeedSubscriptionCriteria()
                .setId(id)
                .setUserId(principal.getId());

        FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
        List<FeedSubscriptionDto> feedSubscriptionList = feedSubscriptionDao.findByCriteria(feedSubscriptionCriteria);
        if (feedSubscriptionList.isEmpty()) {
            throw new ClientException("SubscriptionNotFound", MessageFormat.format("Subscription not found: {0}", id));
        }
        FeedSubscriptionDto feedSubscription = feedSubscriptionList.iterator().next();

        // Get the articles
        UserArticleDao userArticleDao = new UserArticleDao();
        UserArticleCriteria userArticleCriteria = new UserArticleCriteria()
                .setUnread(unread)
                .setUserId(principal.getId())
                .setSubscribed(true)
                .setVisible(true)
                .setFeedId(feedSubscription.getFeedId());
        if (afterArticle != null) {