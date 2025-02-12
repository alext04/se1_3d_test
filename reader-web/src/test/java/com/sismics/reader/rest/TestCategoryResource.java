```java
import com.google.common.collect.ImmutableMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.*;

public class TestCategoryResource extends BaseJerseyTest {

    private static final String TECHNOLOGY_NAME = "technology";
    private static final String COMICS_NAME = "comics";

    @Test
    public void testSuccessfullyTestingCategoryResource() {
        Category category1 = new Category(TECHNOLOGY_NAME);
        Category category2 = new Category(COMICS_NAME);
        Subscription subscription1 = new Subscription("http://localhost:9997/http/feeds/korben.xml");
        Subscription subscription2 = new Subscription("http://localhost:9997/http/feeds/xkcd.xml");

        createUser("category1");
        login("category1");

        // Create category (techno)
        category1 = createCategory(category1);

        // Create category (comics)
        category2 = createCategory(category2);

        // Subscribe to korben.info
        subscription1 = createSubscription(subscription1);

        // Subscribe to xkcd.com
        subscription2 = createSubscription(subscription2);

        // Move the korben.info subscription to "techno"
        moveSubscription(subscription1, category1.getId());

        // Move the xkcd.com subscription to "comics"
        moveSubscription(subscription2, category2.getId());

        // Update category (technology)
        updateCategory(category1, true);

        // Mark all articles in the technology category as read
        markAllArticlesAsRead(category1.getId());

        // Delete category (technology)
        deleteCategory(category1.getId());
    }

    private Category createCategory(Category category) {
        JSONObject json = makePutRequest("/category", category.toJson());
        assertIsOk();
        return new Category(json);
    }

    private Subscription createSubscription(Subscription subscription) {
        JSONObject json = makePutRequest("/subscription", subscription.toJson());
        assertIsOk();
        return new Subscription(json);
    }

    private void moveSubscription(Subscription subscription, String categoryId) {
        JSONObject json = makePostRequest("/subscription/" + subscription.getId(), ImmutableMap.of("category", categoryId));
        assertIsOk();
        assertEquals("ok", json.optString("status"));
    }

    private void updateCategory(Category category, boolean folded) {
        JSONObject json = makePostRequest("/category/" + category.getId(), category.toJson().put("folded", Boolean.toString(folded)));
        assertIsOk();
        assertEquals("ok", json.optString("status"));
        category.setName(TECHNOLOGY_NAME);
        category.setFolded(folded);
    }

    private void markAllArticlesAsRead(String categoryId) {
        makePostRequest("/category/" + categoryId + "/read");
        assertIsOk();
    }

    private void deleteCategory(String categoryId) {
        JSONObject json = makeDeleteRequest("/category/" + categoryId);
        assertIsOk();
        assertEquals("ok", json.optString("status"));
    }

    private static class Category {
        private String id;
        private String name;
        private boolean folded;

        public Category(String name) {
            this.name = name;
        }

        public Category(JSONObject json) {
            this.id = json.optString("id");
            this.name = json.optString("name");
            this.folded = json.optBoolean("folded");
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isFolded() {
            return folded;
        }

        public void setFolded(boolean folded) {
            this.folded = folded;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("name", name);
                if (folded) {
                    json.put("folded", folded);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return json;
        }
    }

    private static class Subscription {
        private String id;
        private String url;

        public Subscription(String url) {
            this.url = url;
        }

        public Subscription(JSONObject json) {
            this.id = json.optString("id");
            this.url = json.optString("url");
        }

        public String getId() {
            return id;
        }

        public String getUrl() {
            return url;
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("url", url);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            return json;
        }
    }
}
```