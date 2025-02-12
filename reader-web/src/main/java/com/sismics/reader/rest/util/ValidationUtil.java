```java
import com.sismics.reader.core.dao.jpa.LocaleDao;
import com.sismics.reader.core.model.jpa.Locale;
import com.sismics.reader.rest.dao.ThemeDao;
import org.apache.commons.lang.StringUtils;

import javax.servlet.ServletContext;
import java.text.MessageFormat;
import java.util.List;

/**
 * Utility class to validate parameters.
 *
 * @author jtremeaux
 */
public class ValidationUtil {

    private static final String ERROR_FIELD_IS_REQUIRED = "{0} is required";
    private static final String ERROR_INVALID_FIELD = "Invalid {0}";

    /**
     * Validates a field.
     *
     * @param servletContext Servlet context
     * @param value The value to validate
     * @param name Name of the parameter
     * @param nullable True if the parameter can be empty or null
     * @return The validated value
     */
    public static String validateParameter(ServletContext servletContext, String value, String name, boolean nullable) {
        if (StringUtils.isEmpty(value)) {
            if (!nullable) {
                throw new ClientException("ValidationError", MessageFormat.format(ERROR_FIELD_IS_REQUIRED, name));
            } else {
                return null;
            }
        }

        value = StringUtils.strip(value);
        return value;
    }

    /**
     * Validates a theme.
     *
     * @param servletContext Servlet context
     * @param themeId ID of the theme to validate
     * @param name Name of the parameter
     * @return The validated theme ID
     */
    public static String validateTheme(ServletContext servletContext, String themeId, String name) {
        themeId = validateParameter(servletContext, themeId, name, true);

        if (StringUtils.isEmpty(themeId)) {
            return null;
        }

        ThemeDao themeDao = new ThemeDao();
        List<String> themeList = themeDao.findAll(servletContext);
        if (!themeList.contains(themeId)) {
            throw new ClientException("ValidationError", MessageFormat.format(ERROR_INVALID_FIELD, name));
        }

        return themeId;
    }

    /**
     * Validates a locale.
     *
     * @param localeId ID of the locale to validate
     * @param name Name of the parameter
     * @return The validated locale ID
     */
    public static String validateLocale(String localeId, String name) {
        localeId = validateParameter(servletContext, localeId, name, true);

        if (StringUtils.isEmpty(localeId)) {
            return null;
        }

        LocaleDao localeDao = new LocaleDao();
        Locale locale = localeDao.getById(localeId);
        if (locale == null) {
            throw new ClientException("ValidationError", MessageFormat.format(ERROR_INVALID_FIELD, name));
        }

        return localeId;
    }
}
```