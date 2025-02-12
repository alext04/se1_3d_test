**Refactored Code:**

```java
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import staticjunit.framework.Assert.assertEquals;
import staticjunit.framework.Assert.assertNotNull;

/**
 * Exhaustive test of the starred resource.
 *
 * @author jtremeaux
 */
public class TestStarredResource extends BaseJerseyTest {

    private static final String URL_SUBSCRIPTION = "/subscription";
    private static final String URL_ALL = "/all";
    private static final String URL_STARRED = "/starred";
    private static final String URL_STARRED_ADD_ONE = "/starred/";
    private static final String URL_STARRED_ADD_MULTIPLE = "/starred/star";
    private static final String URL_STARRED_DELETE_ONE = "/starred/";
    private static final String URL_STARRED_DELETE_MULTIPLE = "/starred/unstar";

    private static final String PAYLOAD_URL = "url";
    private static final String PAYLOAD_ID = "id";
    private static final String JSON_RESPONSE_ARTICLES = "articles";
    private static final String JSON_RESPONSE_ID = "id";

    /**
     * Test of the all resource.
     */
    @Test
    public void testStarredResource() throws JSONException {
        JSONObject json;
        JSONArray articles;
        String articleId;

        // Create user and subscribe to feed
        String userId = createUserAndSubscribe();

        // Check the all resource
        articles = getArticles(URL_ALL);
        assertEquals(10, articles.length());
        articleId = articles.getJSONObject(0).getString(JSON_RESPONSE_ID);

        // Create starred articles
        addStarredArticle(URL_STARRED_ADD_ONE, articleId);
        addStarredArticle(URL_STARRED_ADD_ONE, articleId);

        // Check starred resource
        articles = getArticles(URL_STARRED);
        assertEquals(2, articles.length());

        // Check pagination
        articles = getArticles(URL_STARRED, ImmutableMap.of("after_article", articleId));
        assertEquals(1, articles.length());

        // Delete starred article
        deleteStarredArticle(URL_STARRED_DELETE_ONE, articleId);

        // Check starred resource
        articles = getArticles(URL_STARRED);
        assertEquals(1, articles.length());

        // Delete multiple starred articles
        deleteStarredArticles(URL_STARRED_DELETE_MULTIPLE, ImmutableMultimap.of(PAYLOAD_ID, articleId));

        // Check starred resource
        articles = getArticles(URL_STARRED);
        assertEquals(0, articles.length());

        // Create multiple starred article
        addStarredArticles(URL_STARRED_ADD_MULTIPLE, ImmutableMultimap.of(PAYLOAD_ID, articleId));

        // Check starred resource
        articles = getArticles(URL_STARRED);
        assertEquals(2, articles.length());
    }

    private void addStarredArticle(String url, String articleId) throws JSONException {
        PUT(url + articleId);
        assertStatusCodeIsOk();
    }

    private void addStarredArticles(String url, ImmutableMultimap<String, String> payload) throws JSONException {
        POST(url, payload);
        assertStatusCodeIsOk();
    }

    private void deleteStarredArticle(String url, String articleId) throws JSONException {
        DELETE(url + articleId);
        assertStatusCodeIsOk();
    }

    private void deleteStarredArticles(String url, ImmutableMultimap<String, String> payload) throws JSONException {
        POST(url, payload);
        assertStatusCodeIsOk();
    }

    private String createUserAndSubscribe() throws JSONException {
        JSONObject json;
        String subscriptionId;

        // Create user
        String userId = createUser("starred1");
        login(userId);

        // Subscribe to feed
        PUT(URL_SUBSCRIPTION, ImmutableMap.of(PAYLOAD_URL, "http://localhost:9997/http/feeds/korben.xml"));
        assertStatusCodeIsOk();
        json = getJsonResult();
        subscriptionId = json.optString(JSON_RESPONSE_ID);
        assertNotNull(subscriptionId);

        return userId;
    }

    private JSONArray getArticles(String url) throws JSONException {
        GET(url);
        assertStatusCodeIsOk();
        return getJsonResult().optJSONArray(JSON_RESPONSE_ARTICLES);
    }

    private JSONArray getArticles(String url, ImmutableMap<String, String> params) throws JSONException {
        GET(url, params);
        assertStatusCodeIsOk();
        return getJsonResult().optJSONArray(JSON_RESPONSE_ARTICLES);
    }
}
```