package com.sismics.reader.rest.util;

import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.security.IPrincipal;
import com.sismics.security.UserPrincipal;
import com.sismics.util.filter.SecurityFilter;
import org.codehaus.jettison.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Set;

public class ResourceHelper {

    /**
     * Retrieves the authenticated principal from the HTTP request.
     */
    public static IPrincipal getPrincipal(HttpServletRequest request) {
        Principal principal = (Principal) request.getAttribute(SecurityFilter.PRINCIPAL_ATTRIBUTE);
        if (principal instanceof IPrincipal) {
            return (IPrincipal) principal;
        }
        return null;
    }
    
    /**
     * Returns true if the HTTP request is authenticated.
     */
    public static boolean authenticate(HttpServletRequest request) {
        IPrincipal principal = getPrincipal(request);
        return principal != null && !principal.isAnonymous();
    }
    
    /**
     * Checks if the current user has the specified base function.
     * Throws ForbiddenClientException if the check fails.
     */
    public static void checkBaseFunction(HttpServletRequest request, BaseFunction baseFunction) throws JSONException {
        if (!hasBaseFunction(request, baseFunction)) {
            throw new ForbiddenClientException();
        }
    }
    
    /**
     * Returns true if the current user has the given base function.
     */
    public static boolean hasBaseFunction(HttpServletRequest request, BaseFunction baseFunction) throws JSONException {
        IPrincipal principal = getPrincipal(request);
        if (principal == null || !(principal instanceof UserPrincipal)) {
            return false;
        }
        Set<String> baseFunctionSet = ((UserPrincipal) principal).getBaseFunctionSet();
        return baseFunctionSet != null && baseFunctionSet.contains(baseFunction.name());
    }
}
