**Refactored Code:**

**ThemeConstants.java:**
```java
public class ThemeConstants {
    public static final String[] STYLESHEETS_THEME_DIRECTORIES = { "/src/stylesheets/theme/", "/stylesheets/theme/" };
}
```

**ThemeDao.java:**
```java
import com.google.common.collect.Lists;
import com.google.common.io.Files;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Theme DAO.
 *
 * @author jtremeaux 
 */
public class ThemeDao {
    private static final FilenameFilter CSS_FILTER = (dir, name) -> name.endsWith(".css") || name.endsWith(".less");

    /**
     * Return the list of all themes.
     *
     * @param servletContext Servlet context
     * @return List of themes
     */
    public List<String> findAll(ServletContext servletContext) {
        List<String> themeList = new ArrayList<>();
        
        for (String themeDir : ThemeConstants.STYLESHEETS_THEME_DIRECTORIES) {
            addThemesFromDirectory(themeList, servletContext, themeDir);
        }
        
        return themeList;
    }
    
    private void addThemesFromDirectory(List<String> themeList, ServletContext servletContext, String themeDir) {
        if (servletContext != null) {
            Set<String> fileList = servletContext.getResourcePaths(themeDir);
            if (fileList != null) {
                for (String file : fileList) {
                    if (CSS_FILTER.accept(null, file)) {
                        themeList.add(Files.getNameWithoutExtension(new File(file).getName()));
                    }
                }
            }
        } else {
            URL resource = this.getClass().getResource(themeDir);
            if (resource != null) {
                File dir = new File(resource.getFile());
                for (File file : dir.listFiles(CSS_FILTER)) {
                   themeList.add(Files.getNameWithoutExtension(file.getName()));
                }
            }
        }
    }
}
```