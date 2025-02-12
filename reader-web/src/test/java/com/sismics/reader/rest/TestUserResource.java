**Refactored Code**:
```java
package com.sismics.reader.rest;

import com.google.common.collect.ImmutableMap;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import static junit.framework.Assert.*;

/**
 * Exhaustive test of the user resource.
 *
 * @author jtremeaux
 */
public class TestUserResource extends BaseJerseyTest {

    @Test
    public void testUserResourceAnonymous() throws JSONException {
        // Check anonymous user information
        GET("/user");
        assertIsOk();
        JSONObject json = getJsonResult();
        assertTrue(json.getBoolean("is_default_password"));
    }

    @Test
    public void testUserResourceFunctions() throws JSONException {
        // Create a user
        final String username = "bob";
        createUser(username);

        // Login as a user
        login(username);

        // Perform user operations
        updateUser(username);
        deleteUser(username);
    }

    @Test
    public void testValidationError() {
        // Create a user with invalid data
        POST("/user", ImmutableMap.of(
                "username", "invalid_username",
                "email", "invalid_email",
                "password", "invalid_password"));
        assertIsBadRequest();
        JSONObject json = getJsonResult();
        assertEquals("ValidationError", json.getString("type"));
    }

    @Test
    public void testUserCheckUsername() throws JSONException {
        // Check if a username is free
        GET("/user/check_username", ImmutableMap.of("username", "carol"));
        assertIsOk();
    }

    @Test
    public void testUserLogin() throws JSONException {
        // Login as a user
        login("alice");
        assertIsOk();
    }

    @Test
    public void testUserAdminFunctions() throws JSONException {
        // Create a user
        final String username = "admin_user1";
        createUser(username);

        // Login as admin
        login("admin", "admin");
        assertIsOk();

        // Perform admin operations
        updateUser(username, ImmutableMap.of("first_connection", "false"));
        deleteUser(username);
    }

    private void createUser(String username) throws JSONException {
        // Create a user
        POST("/user", ImmutableMap.of(
                "username", username,
                "email", username + "@example.com",
                "password", "12345678"));
        assertIsOk();
    }

    private void login(String username) throws JSONException {
        // Login as a user
        POST("/user/login", ImmutableMap.of(
                "username", username,
                "password", "12345678"));
        assertIsOk();
    }

    private void updateUser(String username) throws JSONException {
        // Update user information
        POST("/user", ImmutableMap.<String, String>builder()
                .put("email", username + "@example.com")
                .put("theme", "highcontrast")
                .put("locale", "en")
                .put("display_title_web", "true")
                .put("display_title_mobile", "false")
                .put("display_unread_web", "false")
                .put("display_unread_mobile", "false")
                .put("narrow_article", "true")
                .build());
        assertIsOk();
        JSONObject json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    private void updateUser(String username, ImmutableMap<String, String> properties) throws JSONException {
        // Update user information
        POST("/user", properties);
        assertIsOk();
        JSONObject json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }

    private void deleteUser(String username) throws JSONException {
        // Delete user
        DELETE("/user/" + username);
        assertIsOk();
        JSONObject json = getJsonResult();
        assertEquals("ok", json.getString("status"));
    }
}
```