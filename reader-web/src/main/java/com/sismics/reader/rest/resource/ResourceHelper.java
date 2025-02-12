Refactored Code:

```java
package com.sismics.reader.rest.util;

import com.sismics.reader.rest.constant.BaseFunction;
import com.sismics.rest.exception.ForbiddenClientException;
import com.sismics.security.IPrincipal;
import org.codehaus.jettison.json.JSONException;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;

public class ResourceHelper {

    public static IPrincipal getPrincipal(HttpServletRequest request) {
        return (IPrincipal) request.getAttribute(SecurityFilter.PRINCIPAL_ATTRIBUTE);
    }

    public static boolean isAuthenticated(HttpServletRequest request) {
        IPrincipal principal = getPrincipal(request);
        return principal != null && !principal.isAnonymous();
    }

    public static void checkBaseFunction(HttpServletRequest request, BaseFunction baseFunction) throws JSONException {
        if (!hasBaseFunction(request, baseFunction)) {
            throw new ForbiddenClientException();
        }
    }

    public static boolean hasBaseFunction(HttpServletRequest request, BaseFunction baseFunction) throws JSONException {
        IPrincipal principal = getPrincipal(request);
        if (principal == null || !(principal instanceof UserPrincipal)) {
            return false;
        }
        Set<String> baseFunctionSet = ((UserPrincipal) principal).getBaseFunctionSet();
        return baseFunctionSet != null && baseFunctionSet.contains(baseFunction.name());
    }
}
```