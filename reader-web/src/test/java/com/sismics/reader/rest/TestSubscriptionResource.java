```java
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.sismics.reader.core.model.context.AppContext;
import com.sun.jersey.multipart.FormDataBodyPart;
import com.sun.jersey.multipart.FormDataMultiPart;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;

import static junit.framework.Assert.*;

/**
 * Exhaustive test of the subscription resource.
 * 
 * @author jtremeaux
 */
public class TestSubscriptionResource extends BaseJerseyTest {
    private static final String SUBSCRIPTION1_ID = "subscription1Id";
    private static final String ARTICLE_1_ID = "article0Id";
    private static final String ARTICLE_2_ID = "article1Id";
    private static final String ARTICLE_3_ID = "article2Id";

    /**
     * Test of the subscription add resource.
     * 
     */
    @Test
    public void testSubscriptionAddResource() throws JSONException {
        // Create user subscription1
        createUser("subscription1");
        login("subscription1");

        // Create a category
        final String TECHNO_CATEGORY_ID = createCategory("techno");
        assertNotNull(TECHNO_CATEGORY_ID);

        // Subscribe to korben.info
        final String SUBSCRIPTION1_URL = "http://localhost:9997/http/feeds/korben.xml";
        final String SUBSCRIPTION1_RESPONSE = createSubscription(SUBSCRIPTION1_URL);
        JSONObject json = new JSONObject(SUBSCRIPTION1_RESPONSE);
        String subscription1Id = json.optString("id");
        assertNotNull(subscription1Id);

        // Move the korben.info subscription to "techno"
        POST("/subscription/" + subscription1Id, ImmutableMap.of("category", TECHNO_CATEGORY_ID));
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // List all subscriptions
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        JSONArray subscriptions = json.optJSONArray("subscriptions");
        assertNotNull(subscriptions);
        assertEquals(1, subscriptions.length());
        JSONObject subscription = subscriptions.optJSONObject(0);
        assertEquals(10, subscription.getInt("unread_count"));
        assertEquals(SUBSCRIPTION1_URL, subscription.getString("url"));

        // Check the subscription data
        final String SUBSCRIPTION1_DATA = getSubscriptionData(subscription1Id);
        json = new JSONObject(SUBSCRIPTION1_DATA);
        JSONObject sub = json.optJSONObject("subscription");
        assertNotNull(sub);
        assertEquals("Korben", sub.optString("title"));
        assertEquals("Korben", sub.optString("feed_title"));
        assertEquals("http://korben.info", sub.optString("url"));
        assertEquals("Upgrade your mind", sub.optString("description"));
        assertEquals("http://localhost:9997/http/feeds/korben.xml", sub.optString("rss_url"));
        assertNotNull(sub.optLong("create_date"));
        assertNotNull(sub.optString("category_id"));
        assertEquals("techno", sub.optString("category_name"));
        JSONArray articles = json.optJSONArray("articles");
        assertEquals(10, articles.length());

        // Check pagination
        final String SUBSCRIPTION1_PAGINATION = getSubscriptionData(subscription1Id, ImmutableMap.of("after_article", ARTICLE_2_ID));
        json = new JSONObject(SUBSCRIPTION1_PAGINATION);
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(8, articles.length());

        // Update the subscription
        final String SUBSCRIPTION1_UPDATE = updateSubscription(subscription1Id, ImmutableMap.of(
                "order", Integer.valueOf(1).toString(),
                "title", "Korben.info"
        ));
        json = new JSONObject(SUBSCRIPTION1_UPDATE);
        assertEquals("ok", json.getString("status"));

        // Check the updated subscription data
        final String SUBSCRIPTION1_UPDATED_DATA = getSubscriptionData(subscription1Id);
        json = new JSONObject(SUBSCRIPTION1_UPDATED_DATA);
        sub = json.optJSONObject("subscription");
        assertNotNull(sub);
        assertEquals("Korben.info", sub.optString("title"));
        assertEquals("http://korben.info", sub.optString("url"));
        assertEquals("Upgrade your mind", sub.optString("description"));

        // Marks an article as read
        markArticleRead(ARTICLE_1_ID);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Marks an article as read (2nd time)
        markArticleRead(ARTICLE_1_ID);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Check the subscription data
        final String SUBSCRIPTION1_UNREAD = getSubscriptionData(subscription1Id, ImmutableMap.of("unread", "true"));
        json = new JSONObject(SUBSCRIPTION1_UNREAD);
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(9, articles.length());

        // Check the subscription data
        final String SUBSCRIPTION1_READ = getSubscriptionData(subscription1Id, ImmutableMap.of("unread", "false"));
        json = new JSONObject(SUBSCRIPTION1_READ);
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Check all subscriptions for unread articles
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        assertEquals(9, json.optInt("unread_count"));

        // Marks an article as unread
        markArticleUnread(ARTICLE_1_ID);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Marks an article as unread (2nd time)
        markArticleUnread(ARTICLE_1_ID);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Check the subscription data
        final String SUBSCRIPTION1_UNREAD_AGAIN = getSubscriptionData(subscription1Id, ImmutableMap.of("unread", "true"));
        json = new JSONObject(SUBSCRIPTION1_UNREAD_AGAIN);
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Marks all articles in this subscription as read
        markSubscriptionRead(subscription1Id);
        assertIsOk();

        // Check all subscriptions for unread articles
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        assertEquals(0, json.optInt("unread_count"));

        // Delete the subscription
        DELETE("/subscription/" + subscription1Id);
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    /**
     * Test of the article dates.
     *
     */
    @Test
    public void testArticleDate() throws JSONException {
        // Create a new user
        createUser("subscription2");
        login("subscription2");

        // Subscribe to future date feed
        final String FUTURE_DATE_FEED_URL = "http://localhost:9997/http/feeds/future_date.xml";
        final String SUBSCRIPTION2_RESPONSE = createSubscription(FUTURE_DATE_FEED_URL);
        JSONObject json = new JSONObject(SUBSCRIPTION2_RESPONSE);
        String subscription2Id = json.getString("id");
        assertNotNull(subscription2Id);

        // Check the subscription data
        final String SUBSCRIPTION2_DATA = getSubscriptionData(subscription2Id);
        json = new JSONObject(SUBSCRIPTION2_DATA);
        JSONObject sub = json.optJSONObject("subscription");
        assertNotNull(sub);
        assertEquals("Feed from the future", sub.optString("title"));
        JSONArray articles = json.optJSONArray("articles");
        assertEquals(1, articles.length());
        JSONObject article = articles.optJSONObject(0);
        assertNotNull(article);
        assertTrue(new Date(article.getLong("date")).before(new Date()));
    }

    /**
     * Test of deleted articles.
     *
     */
    @Test
    public void testDeletedArticle() throws Exception {
        // Create a new user
        createUser("subscription3");
        login("subscription3");

        // Subscribe to deleted feed
        copyTempResource("/http/feeds/