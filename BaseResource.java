package com.sismics.reader.rest.resource;

import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.reader.rest.util.ResourceHelper;
import com.sismics.security.IPrincipal;
import org.codehaus.jettison.json.JSONException;

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
     * Checks if the current HTTP request is authenticated.
     * If authenticated, assigns the principal field.
     */
    protected boolean authenticate() {
        boolean ok = ResourceHelper.authenticate(request);
        if (ok) {
            // Set the principal field so that subclasses can use it.
            principal = ResourceHelper.getPrincipal(request);
        }
        return ok;
    }
    
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
    
    /**
     * Checks if the current user has the required base function.
     * Throws ForbiddenClientException if not.
     */
    protected void checkBaseFunction(BaseFunction baseFunction) throws JSONException {
        ResourceHelper.checkBaseFunction(request, baseFunction);
    }
    
    /**
     * Returns true if the current user has the given base function.
     */
    protected boolean hasBaseFunction(BaseFunction baseFunction) throws JSONException {
        return ResourceHelper.hasBaseFunction(request, baseFunction);
    }
}
