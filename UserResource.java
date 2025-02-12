```java
// ...

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

        try {
            User user = createUser(username, password, email, localeId);

            // Raise a user creation event
            UserCreatedEvent userCreatedEvent = new UserCreatedEvent();
            userCreatedEvent.setUser(user);
            AppContext.getInstance().getMailEventBus().post(userCreatedEvent);

            return createResponseJson("ok");
        } catch (Exception e) {
            handleException(e);
        }

        return null; // Will never reach this point
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

        try {
            User user = getActiveUser();
            updateUser(user, password, email, themeId, localeId,
                    displayTitleWeb, displayTitleMobile, displayUnreadWeb,
                    displayUnreadMobile, narrowArticle, firstConnection);

            if (StringUtils.isNotBlank(password)) {
                // Raise a password updated event
                PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
                passwordChangedEvent.setUser(user);
                AppContext.getInstance().getMailEventBus().post(passwordChangedEvent);
            }

            return createResponseJson("ok");
        } catch (Exception e) {
            handleException(e);
        }

        return null; // Will never reach this point
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

        try {
            // Check if the user exists
            User user = getUser(username);

            updateUser(user, password, email, themeId, localeId,
                    displayTitleWeb, displayTitleMobile, displayUnreadWeb,
                    displayUnreadMobile, narrowArticle, null);

            if (StringUtils.isNotBlank(password)) {
                checkBaseFunction(BaseFunction.PASSWORD);

                // Change the password
                user.setPassword(password);
                user = userDao.updatePassword(user);

                // Raise a password updated event
                PasswordChangedEvent passwordChangedEvent = new PasswordChangedEvent();
                passwordChangedEvent.setUser(user);
                AppContext.getInstance().getMailEventBus().post(passwordChangedEvent);
            }

            return createResponseJson("ok");
        } catch (Exception e) {
            handleException(e);
        }

        return null; // Will never reach this point
    }

    // ...
```