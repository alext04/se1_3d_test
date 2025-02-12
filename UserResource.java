**Refactored Code:**

**UserResource.java**

```java
import com.sismics.reader.core.constant.Constants;
import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.dao.jpa.criteria.JobCriteria;
import com.sismics.reader.core.dao.jpa.criteria.JobEventCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserCriteria;
import com.sismics.reader.core.dao.jpa.dto.UserDto;
import com.sismics.reader.core.event.PasswordChangedEvent;
import com.sismics.reader.core.event.UserCreatedEvent;
import com.sismics.reader.core.model.context.AppContext;
import com.sismics.reader.core.model.jpa.AuthenticationToken;
import com.sismics.reader.core.model.jpa.Category;
import com.sismics.reader.core.model.jpa.User;
import com.sismics.reader.core.util.jpa.PaginatedList;
import com.sismics.reader.core.util.jpa.PaginatedLists;
import com.sismics.reader.core.util.jpa.SortCriteria;
import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ClientException;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.rest.exception.ServerException;
import com.sismics.rest.util.ValidationUtil;
import com.sismics.security.UserPrincipal;
import com.sismics.util.EnvironmentUtil;
import com.sismics.util.LocaleUtil;
import com.sismics.util.filter.TokenBasedSecurityFilter;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import javax.servlet.http.Cookie;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Path("/user")
public class UserResource extends BaseResource {
    /**
     * Creates a new user.
     *
     * @param username User's username
     * @param password Password
     * @param email E-Mail
     * @param localeId Locale ID
     * @return Response
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("locale") String localeId,
            @FormParam("email") String email) throws JSONException {

        User user = createUser(username, password, email, localeId);
        AppContext.getInstance().getMailEventBus().post(new UserCreatedEvent(user));
        return Response.ok().entity(createResponse("status", "ok")).build();
    }

    private User createUser(String username, String password, String email, String localeId) throws JSONException {
        checkForbidden(authenticate());
        ensureAdminFunction();

        username = ValidationUtil.validateLength(username, "username", 3, 50);
        ValidationUtil.validateAlphanumeric(username, "username");
        password = ValidationUtil.validateLength(password, "password", 8, 50);
        email = ValidationUtil.validateLength(email, "email", 3, 50);
        ValidationUtil.validateEmail(email, "email");

        User user = new User();
        user.setRoleId(Constants.DEFAULT_USER_ROLE.getValue());
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setDisplayTitleWeb(false);
        user.setDisplayTitleMobile(true);
        user.setDisplayUnreadWeb(true);
        user.setDisplayUnreadMobile(true);
        user.setCreateDate(new Date());

        if (localeId == null) {
            localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
        }
        user.setLocaleId(localeId);

        UserDao userDao = new UserDao();
        String userId;
        try {
            userId = userDao.create(user);
        } catch (Exception e) {
            if ("AlreadyExistingUsername".equals(e.getMessage())) {
                throw new ServerException("AlreadyExistingUsername", "Login already used", e);
            } else {
                throw new ServerException("UnknownError", "Unknown Server Error", e);
            }
        }

        Category category = new Category();
        category.setUserId(userId);
        category.setOrder(0);

        CategoryDao categoryDao = new CategoryDao();
        categoryDao.create(category);

        return user;
    }

    /**
     * Updates user informations.
     *
     * @param password Password
     * @param email E-Mail
     * @param themeId Theme
     * @param localeId Locale ID
     * @param displayTitleWeb Display only article titles (web application).
     * @param displayTitleMobile Display only article titles (mobile application).
     * @param displayUnreadWeb Display only unread titles (web application).
     * @param displayUnreadMobile Display only unread titles (mobile application).
     * @param narrowArticle Narrow article
     * @param firstConnection True if the user hasn't acknowledged the first connection wizard yet.
     * @return Response
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
            @FormParam("password") String password,
            @FormParam("email") String email,
            @FormParam("theme") String themeId,
            @FormParam("locale") String localeId,
            @FormParam("display_title_web") Boolean displayTitleWeb,
            @FormParam("display_title_mobile") Boolean displayTitleMobile,
            @FormParam("display_unread_web") Boolean displayUnreadWeb,
            @FormParam("display_unread_mobile") Boolean displayUnreadMobile,
            @FormParam("narrow_article") Boolean narrowArticle,
            @FormParam("first_connection") Boolean firstConnection) throws JSONException {

        checkForbidden(authenticate());

        password = ValidationUtil.validateLength(password, "password", 8, 50);
        email = ValidationUtil.validateLength(email, "email", null, 100);
        localeId = ValidationUtil.validateLocale(localeId, "locale", true);
        themeId = ValidationUtil.validateTheme(request.getServletContext(), themeId, "theme", true);

        User user = updateUser(principal.getName(), password, email, themeId, localeId, displayTitleWeb, displayTitleMobile,
                displayUnreadWeb, displayUnreadMobile, narrowArticle, firstConnection);

        if (StringUtils.isNotBlank(password)) {
            AppContext.getInstance().getMailEventBus().post(new PasswordChangedEvent(user));
        }

        return Response.ok().entity(createResponse("status", "ok")).build();
    }

    private User updateUser(String username, String password, String email, String themeId, String localeId,
                            Boolean displayTitleWeb, Boolean displayTitleMobile, Boolean displayUnreadWeb, Boolean displayUnreadMobile,
                            Boolean narrowArticle, Boolean firstConnection) throws JSONException {
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            throw new ClientException("UserNotFound", "The user doesn't exist");
        }

        if (email != null) {
            user.setEmail(email);
        }
        if (themeId != null) {
            user.setTheme(themeId);
        }
        if (localeId != null) {
            user.setLocaleId(localeId);
        }
        if (displayTitleWeb != null) {
            user.setDisplayTitleWeb(displayTitleWeb);
        }
        if (displayTitleMobile != null) {
            user.setDisplayTitleMobile(displayTitleMobile);
        }
        if (displayUnreadWeb != null) {
            user.setDisplayUnreadWeb(displayUnreadWeb);
        }
        if (displayUnreadMobile != null) {
            user.setDisplayUnreadMobile(displayUnreadMobile);
        }
        if (narrowArticle != null) {
            user.setNarrowArticle(narrowArticle);
        }
        if (firstConnection != null && hasBaseFunction(BaseFunction.ADMIN)) {
            user.setFirstConnection(firstConnection);
        }

        user = userDao.update(user