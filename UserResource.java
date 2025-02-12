package com.sismics.reader.rest.resource;

import com.sismics.reader.core.constant.Constants;
import com.sismics.reader.core.dao.jpa.*;
import com.sismics.reader.core.dao.jpa.criteria.JobCriteria;
import com.sismics.reader.core.dao.jpa.criteria.JobEventCriteria;
import com.sismics.reader.core.dao.jpa.criteria.UserCriteria;
import com.sismics.reader.core.dao.jpa.dto.JobDto;
import com.sismics.reader.core.dao.jpa.dto.JobEventDto;
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

/**
 * User REST resources.
 *
 * @author jtremeaux
 */
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

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        // Validate the input data
        username = ValidationUtil.validateLength(username, "username", 3, 50);
        ValidationUtil.validateAlphanumeric(username, "username");
        password = ValidationUtil.validateLength(password, "password", 8, 50);
        email = ValidationUtil.validateLength(email, "email", 3, 50);
        ValidationUtil.validateEmail(email, "email");

        // Create the user
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
            // Set the locale from the HTTP headers
            localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
        }
        user.setLocaleId(localeId);

        // Create the user
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

        // Create the root category for this user
        Category category = new Category();
        category.setUserId(userId);
        category.setOrder(0);

        CategoryDao categoryDao = new CategoryDao();
        categoryDao.create(category);

        // Raise a user creation event
        UserCreatedEvent userCreatedEvent = new UserCreatedEvent();
        userCreatedEvent.setUser(user);
        AppContext.getInstance().getMailEventBus().post(userCreatedEvent);

        // Always return OK
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
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

        if (!authenticate()) {
            throw new ForbiddenClientException();
        }

        // Validate the input data
        password = ValidationUtil.validateLength(password, "password", 8, 50, true);
        email = ValidationUtil.validateLength(email, "email", null, 100, true);
        localeId = com.sismics.reader.rest.util.ValidationUtil.validateLocale(localeId, "locale", true);
        themeId = com.sismics.reader.rest.util.ValidationUtil.validateTheme(EnvironmentUtil.isUnitTest() ? null : request.getServletContext(), themeId, "theme", true);

        // Update the user
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(principal.getName());
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

        user = userDao.update(user);

        if (StringUtils.isNotBlank(password)) {
            user.setPassword(