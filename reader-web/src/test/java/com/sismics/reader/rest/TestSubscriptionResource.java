**Refactored Code:**

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

/**
 * Exhaustive test of the subscription resource.
 *
 * @author jtremeaux
 */
public class TestSubscriptionResource extends BaseJerseyTest {

    private static final String SUBSCRIPTION_ID_VAR = "subscription_id";
    private static final ImmutableMap<String, String> FEED_KORBEN = ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml");
    private static final String CATEGORY_ID_VAR = "category_id";
    private static final ImmutableMap<String, String> FEED_FUTURE_DATE = ImmutableMap.of("url", "http://localhost:9997/http/feeds/future_date.xml");
    private static final String FEED_SUBSCRIPTION_ID_VAR = "feed_subscription_id";

    @Test
    public void testSubscriptionAddResource() throws JSONException {
        createUser("subscription1");
        login("subscription1");
        createCategory("techno");
        subscribeToKorben();
        moveKorbenToCategory();
        assertSubscriptionCount("techno", 1);
        assertKorbenSubscriptionData();
        assertKorbenArticleCount(10);
        assertSubscriptionArticlePagination();
        updateSubscription();
        assertUpdatedSubscriptionData();
        markArticleRead();
        assertArticleReadCount(9, true);
        assertArticleReadCount(10, false);
        assertUnreadSubscriptionCount(9);
        markArticleUnread();
        assertArticleReadCount(10, true);
        markAllArticlesRead();
        assertUnreadSubscriptionCount(0);
        deleteSubscription();
    }

    @Test
    public void testArticleDate() throws JSONException {
        createUser("subscription2");
        login("subscription2");
        subscribeToFutureDateFeed();
        assertArticleFutureDate();
    }

    @Test
    public void testDeletedArticle() throws Exception {
        createUser("subscription3");
        login("subscription3");
        subscribeToDeletedFeed();
        assertDeletedArticleCount(3);
        assertUnreadSubscriptionCount(3);
        updateDeletedFeed();
        assertDeletedArticleCount(2);
        assertUnreadSubscriptionCount(2);
        updateDeletedFeed();
        assertDeletedArticleCount(3);
        assertUnreadSubscriptionCount(3);
    }

    @Test
    public void testSubscriptionSynchronizationResource() throws Exception {
        createUser("subscription_sync");
        login("subscription_sync");
        subscribeToKorben();
        synchronizeAllFeeds();
        assertSynchronizationCount(0);
        synchronizeAllFeeds();
        assertSubscriptionSynchronizationSuccess();
        assertSynchronizationErrorCount(0);
    }

    @Test
    public void testSubscriptionImportOpml() throws Exception {
        createUser("import_opml1");
        login("import_opml1");
        importOpml();
        assertCategoryCount(1);
        assertSubscriptionCount("Dev", 1);
    }

    @Test
    public void testSubscriptionImportTakeout() throws Exception {
        createUser("import_takeout1");
        login("import_takeout1");
        importTakeout();
        assertCategoryCount(1);
        assertSubscriptionCount("Blogs", 1);
        assertStarredArticleCount(3);
    }

    // Helper methods

    private void createUser(String username) {
        POST("/user", ImmutableMap.of("username", username));
        assertIsOk();
    }

    private void login(String username) {
        POST("/user/login", ImmutableMap.of("username", username, "password", "test"));
        assertIsOk();
    }

    private void createCategory(String name) {
        PUT("/category", ImmutableMap.of("name", name));
        assertIsOk();
        JSONObject json = getJsonResult();
        String categoryId = json.optString("id");
        assertNotNull(categoryId);
    }

    private void subscribeToKorben() {
        PUT("/subscription", FEED_KORBEN);
        assertIsOk();
        JSONObject json = getJsonResult();
        String subscriptionId = json.getString("id");
        assertNotNull(subscriptionId);
    }

    private void moveKorbenToCategory() {
        POST("/subscription/" + path(SUBSCRIPTION_ID_VAR), ImmutableMap.of("category", path(CATEGORY_ID_VAR)));
        assertIsOk();
        JSONObject json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    private void assertSubscriptionCount(String categoryName, int expectedCount) throws JSONException {
        GET("/subscription");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray categories = json.optJSONArray("categories");
        assertNotNull(categories);
        assertEquals(1, categories.length());
        JSONObject rootCategory = categories.optJSONObject(0);
        categories = rootCategory.getJSONArray("categories");
        JSONObject technoCategory = categories.optJSONObject(0);
        assertEquals(categoryName, technoCategory.getString("name"));
        JSONArray subscriptions = technoCategory.optJSONArray("subscriptions");
        assertEquals(expectedCount, subscriptions.length());
    }

    private void assertKorbenSubscriptionData() throws JSONException {
        GET("/subscription/" + path(SUBSCRIPTION_ID_VAR));
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONObject subscription = json.optJSONObject("subscription");
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
    }

    private void assertKorbenArticleCount(int expectedCount) throws JSONException {
        GET("/subscription/" + path(SUBSCRIPTION_ID_VAR));
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.optJSONArray("articles");
        assertEquals(expectedCount, articles.length());
    }

    private void assertSubscriptionArticlePagination() throws JSONException {
        GET("/subscription/" + path(SUBSCRIPTION_ID_VAR), ImmutableMap.of("after_article", getArticleId(1)));
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.optJSONArray("articles");
        assertNotNull(articles);
        assertEquals(8, articles.length());
        assertEquals(getArticleId(2), articles.getJSONObject(0).getString("id"));
    }

    private void updateSubscription() {
        POST("/subscription/" + path(SUBSCRIPTION_ID_VAR), ImmutableMap.of(
                "order", "1",
                "title", "Korben.info"
        ));
        assertIsOk();
        JSONObject json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    private void assertUpdatedSubscriptionData() throws JSONException {
        GET("/subscription/" + path(SUBSCRIPTION_ID_VAR));
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONObject subscription = json.optJSONObject("subscription");
        assertNotNull(subscription);
        assertEquals("Korben.info", subscription.optString("title"));
        assertEquals("http://korben.info", subscription.optString("url"));
        assertEquals("Upgrade your mind", subscription.optString("description"));
    }

    private void markArticleRead() {
        POST("/article/" + getArticleId(0) + "/read");
        assertIsOk();
        JSONObject json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    private void assertArticleReadCount(int expectedCount, boolean unread) throws JSONException {
        GET("/subscription/" + path(SUBSCRIPTION_ID_VAR), ImmutableMap.of("unread", String.valueOf(unread)));
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.optJSONArray("