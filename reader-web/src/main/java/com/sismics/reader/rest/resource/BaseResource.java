```java
package com.sismics.reader.rest.resource;

import com.sismics.reader.rest.util.ResourceHelper;
import com.sismics.security.IPrincipal;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

/**
 * BaseResource now delegates common methods to ResourceHelper and retains a principal field.
 */
public abstract class BaseResource {

    @Context
    protected HttpServletRequest request;

    @QueryParam("app_key")
    protected String appKey;

    // Reintroduce the principal field so that code referencing 'principal' compiles.
    protected IPrincipal principal;

    /**
     * Returns the authenticated principal.
     */
    protected IPrincipal getPrincipal() {
        // Return the field if already set, otherwise fetch it.
        if (principal == null) {
            principal = ResourceHelper.getPrincipal(request);
        }
        return principal;
    }

    // Delegate authentication and base function checks to ResourceHelper.

    // Other methods...
}
```