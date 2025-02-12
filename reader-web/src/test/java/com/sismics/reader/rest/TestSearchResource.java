```java
package com.sismics.reader.rest;

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

    private static final String USER_1 = "search1";
    private static final String USER_2 = "search2";
    private static final String USER_3 = "search3";

    // User management

    @Test
    public void testCreateUser() throws Exception {
        createUser(USER_1);
        assertIsOk();
    }

    @Test
    public void testLogin() throws Exception {
        login(USER_1);
        assertIsOk();
    }

    // Subscription management

    @Test
    public void testCreateSubscription() throws Exception {
        PUT("/subscription", ImmutableMap.of("url", "http://localhost:9997/http/feeds/korben.xml"));
        assertIsOk();
    }

    // Search functionality

    @Test
    public void testSearchZelda() throws Exception {
        searchAndAssertResults("/search/searchtermzelda", 1, "searchtermZelda prend les armes");
    }

    @Test
    public void testSearchNotFound() throws Exception {
        searchAndAssertResults("/search/njloinzejrmklsjd", 0);
    }

    @Test
    public void testSearchWifi() throws Exception {
        searchAndAssertResults("/search/searchtermwifi", 2, "searchtermwifi sur un téléphone Android", "searchtermWiFi avec vos amis");
    }

    @Test
    public void testSearchGoogleKeep() throws Exception {
        searchAndAssertResults("/search/searchtermgoogle%20searchtermkeep", 2, "searchtermGoogle searchtermKeep…eut pas vraiment en faire plus (pour le moment)", "searchtermZelda prend les armes");
    }

    @Test
    public void testSearchMultiUser() throws Exception {
        searchAndAssertResults("/search/searchtermzelda", 1, "searchtermZelda prend les armes", USER_1, USER_3);
        searchAndAssertResults("/search/searchtermzelda", 0, USER_2);
    }

    @Test
    public void testSearchSubscriptionUpdate() throws Exception {
        searchAndAssertResults("/search/searchtermgoogle%20searchtermkeep", 2, "searchtermGoogle searchtermKeep…eut pas vraiment en faire plus (pour le moment)", "searchtermZelda prend les armes", USER_1, USER_2);
    }

    // Assertion methods

    private void searchAndAssertResults(String url, int expectedCount, String... expectedTitles) throws Exception {
        GET(url);
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray articles = json.getJSONArray("articles");
        assertEquals(expectedCount, articles.length());
        for (int i = 0; i < expectedCount; i++) {
            assertSearchResult(articles, expectedTitles[i], i);
        }
    }

    private void assertSearchResult(JSONArray articles, String title, int index) throws JSONException {
        JSONObject article = articles.getJSONObject(index);
        assertEquals(title, article.getString("title"));
    }
}
```