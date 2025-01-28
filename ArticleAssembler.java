**Refactored Code:**

```java
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.sismics.reader.core.dao.jpa.dto.UserArticleDto;

public class ArticleAssembler {

    private static final String ID_KEY = "id";
    private static final String TITLE_KEY = "title";
    private static final String URL_KEY = "url";
    private static final String DATE_KEY = "date";
    private static final String CREATOR_KEY = "creator";
    private static final String DESCRIPTION_KEY = "description";
    private static final String COMMENT_URL_KEY = "comment_url";
    private static final String COMMENT_COUNT_KEY = "comment_count";
    private static final String SUBSCRIPTION_KEY = "subscription";
    private static final String ENCLOSURE_KEY = "enclosure";
    private static final String IS_READ_KEY = "is_read";
    private static final String IS_STARRED_KEY = "is_starred";

    public static JSONObject asJson(UserArticleDto userArticle) throws JSONException {
        JSONObject userArticleJson = new JSONObject();

        addBaseArticleInfo(userArticleJson, userArticle);
        addSubscriptionInfo(userArticleJson, userArticle);
        addEnclosureInfo(userArticleJson, userArticle);
        addReadAndStarredStatus(userArticleJson, userArticle);

        return userArticleJson;
    }

    private static void addBaseArticleInfo(JSONObject userArticleJson, UserArticleDto userArticle) throws JSONException {
        userArticleJson.put(ID_KEY, userArticle.getId());
        userArticleJson.put(TITLE_KEY, userArticle.getArticleTitle());
        userArticleJson.put(URL_KEY, userArticle.getArticleUrl());
        userArticleJson.put(DATE_KEY, userArticle.getArticlePublicationTimestamp());
        userArticleJson.put(CREATOR_KEY, userArticle.getArticleCreator());
        userArticleJson.put(DESCRIPTION_KEY, userArticle.getArticleDescription());
        userArticleJson.put(COMMENT_URL_KEY, userArticle.getArticleCommentUrl());
        userArticleJson.put(COMMENT_COUNT_KEY, userArticle.getArticleCommentCount());
    }

    private static void addSubscriptionInfo(JSONObject userArticleJson, UserArticleDto userArticle) throws JSONException {
        JSONObject subscription = new JSONObject();
        subscription.put(ID_KEY, userArticle.getFeedSubscriptionId());
        subscription.put(TITLE_KEY, userArticle.getFeedSubscriptionTitle() != null ?
                userArticle.getFeedSubscriptionTitle() : userArticle.getFeedTitle());
        userArticleJson.put(SUBSCRIPTION_KEY, subscription);
    }

    private static void addEnclosureInfo(JSONObject userArticleJson, UserArticleDto userArticle) throws JSONException {
        if (userArticle.getArticleEnclosureUrl() != null) {
            JSONObject enclosure = new JSONObject();
            enclosure.put("url", userArticle.getArticleEnclosureUrl());
            enclosure.put("length", userArticle.getArticleEnclosureLength());
            enclosure.put("type", userArticle.getArticleEnclosureType());
            userArticleJson.put(ENCLOSURE_KEY, enclosure);
        }
    }

    private static void addReadAndStarredStatus(JSONObject userArticleJson, UserArticleDto userArticle) throws JSONException {
        userArticleJson.put(IS_READ_KEY, userArticle.getReadTimestamp() != null);
        userArticleJson.put(IS_STARRED_KEY, userArticle.getStarTimestamp() != null);
    }
}
```