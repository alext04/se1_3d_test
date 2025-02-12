**Refactored Code:**

**TestSecurity.java**

```java
import org.junit.Test;

public class TestSecurity extends BaseJerseyTest {

    @Test
    public void testUserOperations() {
        createUser("testsecurity");

        login("testsecurity");

        userPutForbidden();
        userPostOk();
        logoutOk();
        logoutForbidden();

        loginLongLived();

        logout();
    }

    @Test
    public void testHeaderBasedAuthentication() {
        final String userName = "header_auth_test";

        headerBasedAuthForbidden();
        headerBasedAuthOk();
        headerBasedAuthForbiddenError();
    }

    private void headerBasedAuthForbidden() {
        assertEquals(Status.FORBIDDEN.getStatusCode(), resource()
                .path("/user")
                .post(ClientResponse.class)
                .getStatus());
    }

    private void headerBasedAuthOk() {
        assertEquals(Status.OK.getStatusCode(), resource()
                .path("/user")
                .header(HeaderBasedSecurityFilter.AUTHENTICATED_USER_HEADER, "header_auth_test")
                .post(ClientResponse.class)
                .getStatus());
    }

    private void headerBasedAuthForbiddenError() {
        assertEquals(Status.FORBIDDEN.getStatusCode(), resource()
                .path("/user")
                .header(HeaderBasedSecurityFilter.AUTHENTICATED_USER_HEADER, "erroneous_header_auth_test")
                .post(ClientResponse.class)
                .getStatus());
    }

    private void userPostOk() {
        POST("/user", ImmutableMap.of(
                "email", "testsecurity2@reader.com",
                "locale", "en"
        ));
        assertIsOk();
        getJsonResult();
    }

    private void userPutForbidden() {
        PUT("/user");
        assertIsForbidden();
        getJsonResult();
    }

    private void logoutOk() {
        POST("/user/logout");
        assertIsOk();
        assertTrue(StringUtils.isEmpty(getAuthenticationCookie(response)));
    }

    private void logoutForbidden() {
        POST("/user/logout");
        assertIsForbidden();
    }

    private void loginLongLived() {
        login("testsecurity", "12345678", true);
    }
}
```