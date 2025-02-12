**Refactored Code:**
```java
package com.sismics.reader.rest;

import com.google.common.collect.ImmutableMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.*;
import static org.junit.Assert.assertNotNull;

/**
 * Exhaustive test of the all resource.
 *
 * @author jtremeaux
 */
public class TestAllResource extends BaseJerseyTest {

    private static final String ARTICLE1_ID = "article1";
    private static final String ARTICLE2_ID = "article2";

    /**
     * Test of the all resource.
     */
    @Test
    public void testAllResource() throws JSONException {
        // Create user all1
        createUser("all1");
        login("all1");

        // Subscribe to korben.info
        subscribeToKorben();

        // Check the root category
        var rootCategoryId = getRootCategoryId();
        var articles = getArticlesInRootCategory(rootCategoryId);
        var article1Id = articles.get(1).getString("id");
        var article2Id = articles.get(2).getString("id");

        // Check pagination
        var articlesAfterArticle1 = getArticlesAfterArticle(rootCategoryId, article1Id);
        assertEquals(8, articlesAfterArticle1.length());
        assertEquals(article2Id, articlesAfterArticle1.get(0).getString("id"));

        // Check the all resource
        var allArticles = getAllArticles();
        assertNotNull(allArticles);
        assertEquals(10, allArticles.length());
        article1Id = allArticles.get(1).getString("id");
        article2Id = allArticles.get(2).getString("id");

        // Check pagination
        var allArticlesAfterArticle1 = getAllArticlesAfterArticle(article1Id);
        assertEquals(8, allArticlesAfterArticle1.length());
        assertEquals(article2Id, allArticlesAfterArticle1.get(0).getString("id"));

        // Marks all articles as read
        markAllArticlesAsRead();

        // Check the all resource
        allArticles = getAllArticles();
        assertNotNull(allArticles);
        assertEquals(10, allArticles.length());

        // Check in the subscriptions that there are no unread articles left
        var subscriptions = getSubscriptions();
        var subscription0 = subscriptions.getJSONObject(0);
        assertEquals(0, subscription0.optInt("unread_count"));

        // Check the all resource for unread articles
        var allUnreadArticles = getAllUnreadArticles();
        assertNotNull(allUnreadArticles);
        assertEquals(0, allUnreadArticles.length());
    }

    @Test
    public void testMultipleUsers() throws JSONException {
        // Create user multiple1
        createUser("multiple1");
        login("multiple1");

        // Subscribe to korben.info
        subscribeToKorben();

        // Check the all resource
        var allUnreadArticles = getAllUnreadArticles();
        assertNotNull(allUnreadArticles);
        assertEquals(10, allUnreadArticles.length());

        // Create user multiple2
        createUser("multiple2");
        login("multiple2");

        // Subscribe to korben.info (alternative URL)
        var subscriptionId2 = subscribeToKorben(true);

        // Check the all resource
        allUnreadArticles = getAllUnreadArticles();
        assertNotNull(allUnreadArticles);
        assertEquals(10, allUnreadArticles.length());
    }

    private void subscribeToKorben() throws JSONException {
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
    }

    private String subscribeToKorben(boolean alternativeUrl) throws JSONException {
        var url = alternativeUrl ? "http://localhost:9997/http/feeds/korben2.xml" : "http://localhost:9997/http/feeds/korben.xml";
        PUT("/subscription", ImmutableMap.of("url", url));
        assertIsOk();
        var json = getJsonResult();
        return json.optString("id");
    }

    private String getRootCategoryId() throws JSONException {
        GET("/category");
        assertIsOk();
        var json = getJsonResult();
        var categories = json.optJSONArray("categories");
        return categories.optJSONObject(0).optString("id");
    }

    private JSONArray getArticlesInRootCategory(String rootCategoryId) throws JSONException {
        GET("/category/" + rootCategoryId);
        assertIsOk();
        var json = getJsonResult();
        return json.optJSONArray("articles");
    }

    private JSONArray getArticlesAfterArticle(String rootCategoryId, String afterArticleId) throws JSONException {
        GET("/category/" + rootCategoryId, ImmutableMap.of("after_article", afterArticleId));
        assertIsOk();
        var json = getJsonResult();
        return json.optJSONArray("articles");
    }

    private JSONArray getAllArticles() throws JSONException {
        GET("/all");
        assertIsOk();
        var json = getJsonResult();
        return json.optJSONArray("articles");
    }

    private JSONArray getAllArticlesAfterArticle(String afterArticleId) throws JSONException {
        GET("/all", ImmutableMap.of("after_article", afterArticleId));
        assertIsOk();
        var json = getJsonResult();
        return json.optJSONArray("articles");
    }

    private void markAllArticlesAsRead() throws JSONException {
        POST("/all/read");
        assertIsOk();
    }

    private JSONArray getSubscriptions() throws JSONException {
        GET("/subscription", ImmutableMap.of("after_article", ARTICLE1_ID));
        assertIsOk();
        var json = getJsonResult();
        return json.getJSONArray("categories").getJSONObject(0).getJSONArray("subscriptions");
    }

    private JSONArray getAllUnreadArticles() throws JSONException {
        GET("/all", ImmutableMap.of("unread", "true"));
        assertIsOk();
        var json = getJsonResult();
        return json.optJSONArray("articles");
    }
}
```