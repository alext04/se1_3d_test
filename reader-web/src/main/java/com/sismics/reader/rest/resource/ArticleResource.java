**Refactored Code:**

```java
import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.dao.jpa.criteria.ArticleCriteria;
import com.sismics.reader.core.dao.jpa.criteria.FeedSubscriptionCriteria;
import com.sismics.reader.core.dao.jpa.dto.ArticleDto;
import com.sismics.reader.core.dao.jpa.dto.FeedSubscriptionDto;
import com.sismics.reader.core.model.jpa.UserArticle;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

/**
 * Article REST resources.
 */
@Path("/article")
public class ArticleResource extends BaseResource {
    /**
     * Updates the read status of an article.
     *
     * @param id Article ID
     * @param markRead Whether to mark the article as read or unread
     * @return Response
     */
    @POST
    @Path("/{id: [a-z0-9\\-]+}/{markRead: (?i)read|unread}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateReadStatus(
            @PathParam("id") String id,
            @PathParam("markRead") String markRead) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Get the article
        UserArticleDao userArticleDao = new UserArticleDao();
        UserArticle userArticle = userArticleDao.getUserArticle(id, principal.getId());
        if (userArticle == null) {
            throw new ClientException("ArticleNotFound", MessageFormat.format("Article not found: {0}", id));
        }

        boolean isRead = "read".equalsIgnoreCase(markRead);
        Date readDate = isRead ? new Date() : null;
        if (userArticle.getReadDate() != readDate) {
            // Update the article
            userArticle.setReadDate(readDate);
            userArticleDao.update(userArticle);

            // Update the subscriptions
            ArticleDto article = new ArticleDao().findFirstByCriteria(
                    new ArticleCriteria().setId(userArticle.getArticleId()));

            FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
            for (FeedSubscriptionDto feedSubscription : feedSubscriptionDao.findByCriteria(new FeedSubscriptionCriteria()
                    .setFeedId(article.getFeedId())
                    .setUserId(principal.getId()))) {
                if (isRead) {
                    feedSubscriptionDao.updateUnreadCount(feedSubscription.getId(), feedSubscription.getUnreadUserArticleCount() - 1);
                } else {
                    feedSubscriptionDao.updateUnreadCount(feedSubscription.getId(), feedSubscription.getUnreadUserArticleCount() + 1);
                }
            }
        }

        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    /**
     * Updates the read status of multiple articles.
     *
     * @param idList List of article IDs
     * @param markRead Whether to mark the articles as read or unread
     * @return Response
     */
    @POST
    @Path("/{markRead: (?i)read|unread}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateReadStatusMultiple(
            @FormParam("id") List<String> idList,
            @PathParam("markRead") String markRead) throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        boolean isRead = "read".equalsIgnoreCase(markRead);
        Date readDate = isRead ? new Date() : null;

        for (String id : idList) {
            // Get the article
            UserArticleDao userArticleDao = new UserArticleDao();
            UserArticle userArticle = userArticleDao.getUserArticle(id, principal.getId());
            if (userArticle == null) {
                throw new ClientException("ArticleNotFound", MessageFormat.format("Article not found: {0}", id));
            }

            if (userArticle.getReadDate() != readDate) {
                // Update the article
                userArticle.setReadDate(readDate);
                userArticleDao.update(userArticle);

                // Update the subscriptions
                ArticleDto article = new ArticleDao().findFirstByCriteria(
                        new ArticleCriteria().setId(userArticle.getArticleId()));

                FeedSubscriptionDao feedSubscriptionDao = new FeedSubscriptionDao();
                for (FeedSubscriptionDto feedSubscription : feedSubscriptionDao.findByCriteria(new FeedSubscriptionCriteria()
                        .setFeedId(article.getFeedId())
                        .setUserId(principal.getId()))) {
                    if (isRead) {
                        feedSubscriptionDao.updateUnreadCount(feedSubscription.getId(), feedSubscription.getUnreadUserArticleCount() - 1);
                    } else {
                        feedSubscriptionDao.updateUnreadCount(feedSubscription.getId(), feedSubscription.getUnreadUserArticleCount() + 1);
                    }
                }
            }
        }

        // Always return ok
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }
}
```