Refactored Code:

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
import org.junit.Test;
import javax.ws.rs.core.MediaType;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Exhaustive test of the subscription resource.
 * 
 * @author jtremeaux
 */
public class TestSubscriptionResource extends BaseJerseyTest {
    @Test
    public void testSubscriptionAddResource() throws JSONException {
        // Create user subscription1
        createUser("subscription1");
        login("subscription1");
        // Create a category
        PUT("/category", ImmutableMap.of("name", "techno"));
        assertIsOk();
        JSONObject json = getJsonResult();
        String category1Id = json.optString("id");
        assertNotNull(category1Id);
        
        // Subscribe to korben.info
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
        json = getJsonResult();
        String subscription1Id = json.getString("id");
        assertNotNull(subscription1Id);
        
        // Move the korben.info subscription to "techno"
        POST("/subscription/" + subscription1Id, ImmutableMap.of("category", category1Id));
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // List all subscriptions
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        int unreadCount = json.optInt("unread_count");
        assertTrue(unreadCount > 0);
        JSONArray categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        JSONObject rootCategory = categories.optJSONObject(0);
        categories = rootCategory.getJSONArray("categories");
        JSONObject technoCategory = categories.optJSONObject(0);
        JSONArray subscriptions = technoCategory.optJSONArray("subscriptions");
        assertEquals(1, subscriptions.length());
        JSONObject subscription = subscriptions.getJSONObject(0);
        assertEquals(10, subscription.getInt("unread_count"));
        assertEquals("http://localhost:9997/http/feeds/korben.xml", subscription.getString("url"));

        // Check the subscription data
        GET("/subscription/" + subscription1Id);
        assertIsOk();
        json = getJsonResult();
        subscription = json.optJSONObject("subscription");
        assertNotNull(subscription);
        assertEquals("Korben", subscription.optString("title"));
        assertEquals("Korben", subscription.optString("feed_title"));
        assertEquals("http://korben.info", subscription.optString("url"));
        assertEquals("Upgrade your mind", subscription.optString("description"));
        assertEquals("http://localhost:9997/http/feeds/korben.xml", subscription.optString("rss_url"));
        assertNotNull(subscription.optLong("create_date"));
        assertNotNull(subscription.optString("category_id"));
        assertEquals("techno", subscription.optString("category_name"));
        JSONArray articles = json.optJSONArray("articles");
        assertEquals(10, articles.length());
        JSONObject article = articles.optJSONObject(0);
        assertNotNull(article);
        String article0Id = article.getString("id");
        assertNotNull(article0Id);
        JSONObject articleSubscription = article.optJSONObject("subscription");
        assertNotNull(articleSubscription.getString("id"));
        assertNotNull(articleSubscription.getString("title"));
        assertNotNull(article.optString("comment_url"));
        article = (JSONObject) articles.get(1);
        String article1Id = article.getString("id");
        article = (JSONObject) articles.get(2);
        String article2Id = article.getString("id");

        // Check pagination
        GET("/subscription/" + subscription1Id, ImmutableMap.of("after_article", article1Id));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(8, articles.length());
        assertEquals(article2Id, article.getString("id"));

        // Update the subscription
        POST("/subscription/" + subscription1Id, ImmutableMap.of(
                "order", Integer.valueOf(1).toString(),
                "title", "Korben.info"
        ));
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));
        
        // Check the updated subscription data
        GET("/subscription/" + subscription1Id);
        assertIsOk();
        json = getJsonResult();
        subscription = json.optJSONObject("subscription");
        assertNotNull(subscription);
        assertEquals("Korben.info", subscription.optString("title"));
        assertEquals("http://korben.info", subscription.optString("url"));
        assertEquals("Upgrade your mind", subscription.optString("description"));

        // Marks an article as read
        POST("/article/" + article0Id + "/read");
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));
        
        // Marks an article as read (2nd time)
        POST("/article/" + article0Id + "/read");
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Check the subscription data
        GET("/subscription/" + subscription1Id, ImmutableMap.of("unread", "true"));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(9, articles.length());

        // Check the subscription data
        GET("/subscription/" + subscription1Id, ImmutableMap.of("unread", "false"));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Check all subscriptions for unread articles
        GET("/subscription");
        assertIsOk();
        json = getJsonResult();
        assertEquals(9, json.optInt("unread_count"));

        // Marks an article as unread
        POST("/article/" + article0Id + "/unread");
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));
        
        // Marks an article as unread (2nd time)
        POST("/article/" + article0Id + "/unread");
        assertIsOk();
        json = getJsonResult();
        assertEquals("ok", json.getString("status"));

        // Check the subscription data
        GET("/subscription/" + subscription1Id, ImmutableMap.of("unread", "true"));
        assertIsOk();
        json = getJsonResult();
        articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(10, articles.length());

        // Marks all articles in this subscription as read
        POST("/subscription/" + subscription1Id + "/read");
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
  
    @Test
    public void testArticleDate() throws JSONException {
        // Create a new user: OK
        createUser("subscription2");
        login("subscription2");

        // Subscribe to future date feed: OK
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/future_date.xml"));
        assertIsOk();
        JSONObject json = getJsonResult();
        String subscription1Id = json.getString("id");
        assertNotNull(subscription1Id);

        // Check the subscription data
        GET("/subscription/" + subscription1Id);
        assertIsOk();
        json = getJsonResult();
        JSONObject subscription = json.optJSONObject("subscription");
        assertNotNull(subscription);
        assertEquals("Feed from the future", subscription.optString("title"));
        JSONArray articles = json.optJSONArray("articles");
        assertEquals(1,