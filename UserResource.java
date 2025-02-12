```java
@Path("/user")
public class UserResource extends BaseResource {
    // Refactor code smells (long method, duplicated blocks, inappropriate naming)
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

        authenticate();
        checkBaseFunction(BaseFunction.ADMIN);

        User user = authorize();

        if (password != null) {
            user.setPassword(password);
            user = new UserDao().updatePassword(user);
            PasswordChangedEvent event = new PasswordChangedEvent();
            event.setUser(user);
            AppContext.getInstance().getMailEventBus().post(event);
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

        UserDao userDao = new UserDao();
        if (firstConnection != null && hasBaseFunction(BaseFunction.ADMIN)) {
            user.setFirstConnection(firstConnection);
            userDao.update(user);
        } else {
            userDao.updateNoEvent(user);
        }
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }

    // Refactor design smells (large class, god object, circular dependencies, poor abstraction)
    private User authorize() {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        UserDao userDao = new UserDao();
        return userDao.getActiveByUsername(principal.getName());
    }
}
```