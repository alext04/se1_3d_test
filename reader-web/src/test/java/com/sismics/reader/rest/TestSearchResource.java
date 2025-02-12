```java
package com.sismics.reader.rest;

import com.google.common.collect.ImmutableMap;
import junit.framework.Assert;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Unit tests for the search resource
 *
 * @author bgamard
 */
public class TestSearchResource extends BaseJerseyTest {

    /**
     * Test search results with "zelda" keyword
     */
    @Test
    public void testSearchZeldaKeyword() throws Exception {
        // Create a user and subscribe to a feed
        createUserAndSubscribeToFeed("search1", "http://localhost:9997/http/feeds/korben.xml");

        // Search for "zelda"
        JSONArray articles = search("zelda");

        // Assert that one result is returned
        assertEquals(1, articles.length());

        // Assert that the title of the first article matches the expected value
        assertArticleExistsWithTitle(articles, "Quand <span class=\"highlight\">searchtermZelda</span> prend les armes", 0);
    }

    /**
     * Test search results with "njloinzejrmklsjd" keyword
     */
    @Test
    public void testSearchUnexistingKeyword() throws Exception {
        // Create a user and subscribe to a feed
        createUserAndSubscribeToFeed("search1", "http://localhost:9997/http/feeds/korben.xml");

        // Search for "njloinzejrmklsjd"
        JSONArray articles = search("njloinzejrmklsjd");

        // Assert that no result is returned
        assertEquals(0, articles.length());
    }

    /**
     * Test search results with "wifi" keyword
     */
    @Test
    public void testSearchWifiKeyword() throws Exception {
        // Create a user and subscribe to a feed
        createUserAndSubscribeToFeed("search1", "http://localhost:9997/http/feeds/korben.xml");

        // Search for "wifi"
        JSONArray articles = search("wifi");

        // Assert that two results are returned
        assertEquals(2, articles.length());

        // Assert that the titles of the first two articles match the expected values
        assertArticleExistsWithTitle(articles, "Récupérer les clés <span class=\"highlight\">searchtermwifi</span> sur un téléphone Android", 0);
        assertArticleExistsWithTitle(articles, "Partagez vos clés <span class=\"highlight\">searchtermWiFi</span> avec vos amis", 1);
    }

    /**
     * Test search results with "google keep" keyword
     */
    @Test
    public void testSearchGoogleKeepKeyword() throws Exception {
        // Create a user and subscribe to a feed
        createUserAndSubscribeToFeed("search1", "http://localhost:9997/http/feeds/korben.xml");

        // Search for "google keep"
        JSONArray articles = search("google keep");

        // Assert that two results are returned
        assertEquals(2, articles.length());

        // Assert that the titles of the first two articles match the expected values
        assertArticleExistsWithTitle(articles, "<span class=\"highlight\">searchtermGoogle</span> <span class=\"highlight\">searchtermKeep</span>…eut pas vraiment en faire plus (pour le moment)", 0);
        assertArticleExistsWithTitle(articles, "Quand searchtermZelda prend les armes", 1);
    }

    /**
     * Assert that an article exists with a specific title in the provided articles set.
     *
     * @param articles Articles from search
     * @param title Expected title
     * @param index Index of the article
     */
    private void assertArticleExistsWithTitle(JSONArray articles, String title, int index) throws JSONException {
        JSONObject article = articles.getJSONObject(index);
        if (article.getString("title").equals(title)) {
            return;
        }
        Assert.fail("[" + title + "] not found in [" + article.getString("title") + "]");
    }

    /**
     * Create a user and subscribe to a feed.
     *
     * @param username The username of the user to create
     * @param feedUrl The URL of the feed to subscribe to
     */
    private void createUserAndSubscribeToFeed(String username, String feedUrl) throws Exception {
        // Create a user
        createUser(username);

        // Login as the user
        login(username);

        // Subscribe to the feed
        PUT("/subscription", ImmutableMap.of("url", feedUrl));
        assertIsOk();
    }

    /**
     * Search for a given keyword.
     *
     * @param keyword The keyword to search for
     * @return The list of articles matching the keyword
     */
    private JSONArray search(String keyword) throws JSONException {
        // Search for the keyword
        GET("/search/searchterm" + keyword);
        assertIsOk();

        // Get the JSON result
        JSONObject json = getJsonResult();

        // Get the articles from the JSON result
        return json.getJSONArray("articles");
    }
}
```