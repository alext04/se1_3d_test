```java
package com.sismics.reader.rest;

import static javax.ws.rs.core.Response.Status.OK;

import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.junit.Test;

import com.sismics.reader.core.dao.LocaleDao;
import com.sismics.reader.core.model.Locale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test the locale resource.
 * 
 * @author jtremeaux
 */
public class TestLocaleResource extends BaseJerseyTest {
    /**
     * Test the locale resource.
     *
     * @throws JSONException
     */
    @Test
    public void testLocaleResource() throws JSONException {
        GET("/locale");
        assertOk();
        JSONObject json = getJsonResult();
        JSONArray locale = json.getJSONArray("locales");
        assertEquals("incorrect number of locales", 2, locale.length());
    }

    protected void assertIsOk() {
        assertEquals(OK.getStatusCode(), getResponse().getStatus());
    }
}
```