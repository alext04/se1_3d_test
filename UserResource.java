```java
@Path("/user")
public class UserResource extends BaseResource {
    
    // Refactored methods
    
    private Response register(String username, String password, String localeId, String email) throws JSONException {
        // Moved the input validation inside the User creation block
        User user = new User();
        user.setRoleId(Constants.DEFAULT_USER_ROLE.getValue());
        user.setUsername(ValidationUtil.validateLength(username, "username", 3, 50));
        ValidationUtil.validateAlphanumeric(username, "username");
        user.setPassword(ValidationUtil.validateLength(password, "password", 8, 50));
        user.setEmail(ValidationUtil.validateLength(email, "email", 3, 50));
        ValidationUtil.validateEmail(email, "email");
        
        // Moved localeId to default value
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
        CategoryDao categoryDao = new CategoryDao();
        Category category = new Category();
        category.setUserId(userId);
        category.setOrder(0);
        categoryDao.create(category);
        UserCreatedEvent userCreatedEvent = new UserCreatedEvent();
        userCreatedEvent.setUser(user);
        AppContext.getInstance().getMailEventBus().post(userCreatedEvent);
        
        // Always return OK
        JSONObject response = new JSONObject();
        response.put("status", "ok");
        return Response.ok().entity(response).build();
    }
    
    private void handleForbiddenExceptions(Exception e) {
        if ("AlreadyExistingUsername".equals(e.getMessage())) {
            throw new ServerException("AlreadyExistingUsername", "Login already used", e);
        } else {
            throw new ServerException("UnknownError", "Unknown Server Error", e);
        }
    }
    
    private Response checkBaseFunctions(BaseFunction function) {
        if (!hasBaseFunction(function)) {
            throw new ForbiddenClientException();
        }
        return Response.ok().build();
    }
    
    private void validateAndUpdateUser(JSONObject userJson) throws JSONException {
        String username = userJson.getString("username");
        String password = userJson.optString("password");
        String email = userJson.optString("email");
        String themeId = userJson.optString("theme");
        String localeId = userJson.optString("locale");
        Boolean displayTitleWeb = userJson.optBoolean("display_title_web");
        Boolean displayTitleMobile = userJson.optBoolean("display_title_mobile");
        Boolean displayUnreadWeb = userJson.optBoolean("display_unread_web");
        Boolean displayUnreadMobile = userJson.optBoolean("display_unread_mobile");
        Boolean narrowArticle = userJson.optBoolean("narrow_article");
        Boolean firstConnection = userJson.optBoolean("first_connection");

        boolean usernameUpdated = validateAndUpdateUserField(username, "username", 3, 50);
        boolean passwordUpdated = validateAndUpdateUserField(password, "password", 8, 50, true);
        boolean emailUpdated = validateAndUpdateUserField(email, "email", null, 100, false);
        boolean themeUpdated = validateAndUpdateUserField(themeId, "theme", false);
        boolean localeUpdated = validateAndUpdateUserField(localeId, "locale", false);

        if (displayTitleWeb != null) {
            userJson.put("display_title_web", displayTitleWeb);
        }
        if (displayTitleMobile != null) {
            userJson.put("display_title_mobile", displayTitleMobile);
        }
        if (displayUnreadWeb != null) {
            userJson.put("display_unread_web", displayUnreadWeb);
        }
        if (displayUnreadMobile != null) {
            userJson.put("display_unread_mobile", displayUnreadMobile);
        }
        if (narrowArticle != null) {
            userJson.put("narrow_article", narrowArticle);
        }
        if (firstConnection != null && hasBaseFunction(BaseFunction.ADMIN)) {
            userJson.put("first_connection", firstConnection);
        }
    }
    
    private boolean validateAndUpdateUserField(String value, String fieldName, Integer min, Integer max) {
        if (value != null) {
            value = ValidationUtil.validateLength(value, fieldName, min, max);
            return true;
        }
        return false;
    }
    
    private boolean validateAndUpdateUserField(String value, String fieldName, Integer min, Integer max, boolean optional) {
        if (value != null) {
            value = ValidationUtil.validateLength(value, fieldName, min, max);
            return true;
        }
        if (!optional) {
            throw new ClientException("ValidationError", "The " + fieldName + " field cannot be null");
        }
        return false;
    }
    
    private boolean validateAndUpdateUserField(String value, String fieldName, boolean optional) {
        if (value != null) {
            value = ValidationUtil.validateLength(value, fieldName, null, null);
            return true;
        }
        if (!optional) {
            throw new ClientException("ValidationError", "The " + fieldName + " field cannot be null");
        }
        return false;
    }
}
```