```java
import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

public class ArticleAssembler {

    public static JSONObject asJson(UserArticleDto userArticle) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", userArticle.getId());
        json.put("title", userArticle.getArticleTitle());
        json.put("url", userArticle.getArticleUrl());
        json.put("date", userArticle.getArticlePublicationTimestamp());
        json.put("creator", userArticle.getArticleCreator());
        json.put("description", userArticle.getArticleDescription());
        json.put("comment_url", userArticle.getArticleCommentUrl());
        json.put("comment_count", userArticle.getArticleCommentCount());
        addSubscription(json, userArticle);
        addEnclosure(json, userArticle);
        addReadAndStarredStatus(json, userArticle);
        return json;
    }

    private static void addSubscription(JSONObject json, UserArticleDto userArticle) throws JSONException {
        JSONObject subscription = new JSONObject();
        subscription.put("id", userArticle.getFeedSubscriptionId());
        subscription.put("title", userArticle.getFeedSubscriptionTitle() != null ?
            userArticle.getFeedSubscriptionTitle() : userArticle.getFeedTitle());
        json.put("subscription", subscription);
    }

    private static void addEnclosure(JSONObject json, UserArticleDto userArticle) throws JSONException {
        if (userArticle.getArticleEnclosureUrl() != null) {
            JSONObject enclosure = new JSONObject();
            enclosure.put("url", userArticle.getArticleEnclosureUrl());
            enclosure.put("length", userArticle.getArticleEnclosureLength());
            enclosure.put("type", userArticle.getArticleEnclosureType());
            json.put("enclosure", enclosure);
        }
    }

    private static void addReadAndStarredStatus(JSONObject json, UserArticleDto userArticle) throws JSONException {
        json.put("is_read", userArticle.getReadTimestamp() != null);
        json.put("is_starred", userArticle.getStarTimestamp() != null);
    }
}
```