**Refactored Code:**

**BaseJerseyTest.java**

```java
public abstract class BaseJerseyTest {
    
    // ...
    
    protected void login(String username, String password, boolean getHeaders) throws JSONException {
        login(username, password, getHeaders, true);
    }
    
    // ...
}
```

**TestAppResource.java**

```java
import com.google.common.collect.ImmutableMap;
import com.sismics.rest.exception.ClientException;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TestAppResource extends BaseJerseyTest {

    private static final String DEFAULT_URI = "http://example.com:8180/api/";
    
    @Mock
    private AppResource appResource;
    
    @Mock
    private MapPortResource mapPortResource;
    
    @Mock
    private LogResource logResource;
    
    @Context
    private HttpHeaders headers;
    
    @Context
    private UriInfo uriInfo;

    @Before
    public void setUp() {
        setUpResources();
        initMocking();
    }
    
    @Test
    public void testAppResource_checkApplicationInfo() throws JSONException {
        // Check the application info
        GET("/app");
        assertIsOk();
        JSONObject json = getJsonResult();
        String currentVersion = json.getString("current_version");
        assertNotNull(currentVersion);
        String minVersion = json.getString("min_version");
        assertNotNull(minVersion);
        Long freeMemory = json.getLong("free_memory");
        assertTrue(freeMemory > 0);
        Long totalMemory = json.getLong("total_memory");
        assertTrue(totalMemory > 0 && totalMemory > freeMemory);
    }
    
    @Test
    public void testAppResource_rebuildArticlesIndex() throws JSONException {
        // Login admin
        login("admin", "admin", false);
        
        // Rebuild articles index
        POST("/app/batch/reindex");
        assertIsOk();
    }
    
    @Test
    public void testMapPortResource() throws JSONException {
        // Login admin
        login("admin", "admin", false);
        
        // Map port using UPnP
        POST("/app/map_port");
        assertIsOk();
    }
    
    @Test
    public void testLogResource() throws JSONException {
        // Login admin
        login("admin", "admin", false);

        // Generate some error logs
        for (int i = 0; i < 20; i++) {
            new ClientException("type", "some error " + i, null);
        }

        // Check the logs (page 1)
        GET("/app/log", ImmutableMap.of("level", "ERROR"));
        assertIsOk();
        JSONObject json = getJsonResult();
        JSONArray logs = json.getJSONArray("logs");
        assertEquals(10, logs.length());
        Long date1 = logs.optJSONObject(0).optLong("date");
        Long date2 = logs.optJSONObject(9).optLong("date");
        assertTrue(date1 >= date2);
        
        // Check the logs (page 2)
        GET("/app/log", ImmutableMap.of(
                "offset",  "10",
                "level", "ERROR"));
        assertIsOk();
        json = getJsonResult();
        logs = json.getJSONArray("logs");
        assertTrue(logs.length() == 10);
        Long date3 = logs.optJSONObject(0).optLong("date");
        Long date4 = logs.optJSONObject(9).optLong("date");
        assertTrue(date3 >= date4);
    }

    // ...
    
    private void initMocking() {
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GET("/app");
                return null;
            }
        }).when(appResource).getApplicationInfo(any(HttpHeaders.class), any(UriInfo.class));
        
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GET("/app/batch/reindex");
                return null;
            }
        }).when(appResource).rebuildArticlesIndex(any(HttpHeaders.class), any(UriInfo.class));
        
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GET("/app/map_port");
                return null;
            }
        }).when(mapPortResource).mapPort(any(HttpHeaders.class), any(UriInfo.class));
        
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GET("/app/log", new HashMap<String, String>() {{
                    put("level", "ERROR");
                }});
                return null;
            }
        }).when(logResource).getLogs(any(HttpHeaders.class), any(UriInfo.class), any(Map.class));
        
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                GET("/app/log", new HashMap<String, String>() {{
                    put("offset", "10");
                    put("level", "ERROR");
                }});
                return null;
            }
        }).when(logResource).getLogs(any(HttpHeaders.class), any(UriInfo.class), any(Map.class));
    }
}
```