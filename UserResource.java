```java
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

        // Set the locale from the HTTP headers
        String localeId = LocaleUtil.getLocaleIdFromAcceptLanguage(request.getHeader("Accept-Language"));
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
            user.setPassword(password);
            user = userDao.updatePassword(user);
        }
        
        if (StringUtils.isNotBlank(password)) {
            // Raise a password updated event
            PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
            passwordChangedEvent.setUser(user);
            AppContext.getInstance().getMailEventBus().post(passwordChangedEvent);
        }
        
        // Always return "ok"
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    /**
     * Updates user informations.
     * 
     * @param username Username
     * @param password Password
     * @param email E-Mail
     * @param themeId Theme
     * @param localeId Locale ID
     * @param displayTitleWeb Display only article titles (web application).
     * @param displayTitleMobile Display only article titles (mobile application).
     * @param displayUnreadWeb Display only unread titles (web application).
     * @param displayUnreadMobile Display only unread titles (mobile application).
     * @return Response
     */
    @POST
    @Path("{username: [a-zA-Z0-9_]+}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response update(
        @PathParam("username") String username,
        @FormParam("password") String password,
        @FormParam("email") String email,
        @FormParam("theme") String themeId,
        @FormParam("locale") String localeId,
        @FormParam("display_title_web") Boolean displayTitleWeb,
        @FormParam("display_title_mobile") Boolean displayTitleMobile,
        @FormParam("display_unread_web") Boolean displayUnreadWeb,
        @FormParam("display_unread_mobile") Boolean displayUnreadMobile,
        @FormParam("narrow_article") Boolean narrowArticle) throws JSONException {
        
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);
        
        // Validate the input data
        password = ValidationUtil.validateLength(password, "password", 8, 50, true);
        email = ValidationUtil.validateLength(email, "email", null, 100, true);
        localeId = com.sismics.reader.rest.util.ValidationUtil.validateLocale(localeId, "locale", true);
        themeId = com.sismics.reader.rest.util.ValidationUtil.validateTheme(request.getServletContext(), themeId, "theme", true);
        
        // Check if the user exists
        UserDao userDao = new UserDao();
        User user = userDao.getActiveByUsername(username);
        if (user == null) {
            