```java
package com.sismics.reader.rest.constant;

public enum BaseFunction {

    GRANT_ADMIN_FUNCTIONS("Allows the user to use the admin functions"),
    CHANGE_PASSWORD("Allows the user to change his password"),
    IMPORT_DATA("Allows the user to import OPML / Takeout data");

    private final String description;

    BaseFunction(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
```