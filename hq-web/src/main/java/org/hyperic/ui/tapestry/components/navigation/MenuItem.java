package org.hyperic.ui.tapestry.components.navigation;

/**
 * Menu Item descriptor for the Navigation Tab component
 *
 */
public class MenuItem {

    private String name;
    private String url;
    private String iconClass;

    public MenuItem(String name, String url, String iconClass) {
        this.name = name;
        this.url = url;
        this.iconClass = iconClass;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getIconClass() {
        return iconClass;
    }

    public void setIconClass(String iconClass) {
        this.iconClass = iconClass;
    }

}
