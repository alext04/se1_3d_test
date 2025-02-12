```java
import com.google.common.collect.ImmutableMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Exhaustive test of the search resource.
 *
 * @author bgamard
 */
public class TestSearchResource extends BaseJerseyTest {

    @Test
    public void searchWithExistingResults() throws Exception {
        setupTestEnvironment();

        GET("/search/searchtermzelda");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.getJSONArray("articles");
        assertEquals(1, articles.length());
        assertSearchResult(articles, "Quand <span class=\"highlight\">searchtermZelda</span> prend les armes", 0);
    }

    @Test
    public void searchWithNoResults() throws Exception {
        setupTestEnvironment();

        GET("/search/njloinzejrmklsjd");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.getJSONArray("articles");
        assertEquals(0, articles.length());
    }

    @Test
    public void searchWithMultipleResults() throws Exception {
        setupTestEnvironment();

        GET("/search/searchtermwifi");
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.getJSONArray("articles");
        assertEquals(2, articles.length());
        assertSearchResult(articles, "Récupérer les clés <span class=\"highlight\">searchtermwifi</span> sur un téléphone Android", 0);
        assertSearchResult(articles, "Partagez vos clés <span class=\"highlight\">searchtermWiFi</span> avec vos amis", 1);
    }

    private void setupTestEnvironment() throws Exception {
        // Create user search1
        createUser("search1");
        login("search1");

        // Subscribe to Korben RSS feed
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
    }

    /**
     * Assert that an article exists with a specific title in the provided articles set.
     *
     * @param articles Articles from search
     * @param title    Expected title
     * @param index    Index
     */
    private void assertSearchResult(JSONArray articles, String title, int index) throws JSONException {
        JSONObject article = articles.getJSONObject(index);
        if (article.getString("title").equals(title)) {
            return;
        }
        Assert.fail("[" + title + "] not found in [" + article.getString("title") + "]");
    }
}
```