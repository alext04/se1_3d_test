```java
@Path("/user")
public class UserResource extends BaseResource {

    // Renamed `getUserId` to `getCurrentUser`.
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser() throws JSONException {
        if (!authenticate()) {
            throw new ForbiddenClientException();
        }
        checkBaseFunction(BaseFunction.ADMIN);

        JSONObject response = new JSONObject();
        if (!authenticate()) {
            response.put("anonymous", true);
        } else {
            response.put("anonymous", false);
            UserDao userDao = new UserDao();
            User user = userDao.getById(principal.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("theme", user.getTheme());
            response.put("locale", user.getLocaleId());
            response.put("display_title_web", user.isDisplayTitleWeb());
            response.put("display_title_mobile", user.isDisplayTitleMobile());
            response.put("display_unread_web", user.isDisplayUnreadWeb());
            response.put("display_unread_mobile", user.isDisplayUnreadMobile());
            JSONArray baseFunctions = new JSONArray(((UserPrincipal) principal).getBaseFunctionSet());
            response.put("base_functions", baseFunctions);
        }

        return Response.ok().entity(response).build();
    }
}
```