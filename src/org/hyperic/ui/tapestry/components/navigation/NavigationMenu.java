package org.hyperic.ui.tapestry.components.navigation;

import java.util.List;

public class NavigationMenu {

    private List<MenuItem> resourceMenuItems;
    private List<MenuItem> analyzeMenuItems;
    private List<MenuItem> favoriteResourceItems;
    private List<MenuItem> recentResourceItems;

    public NavigationMenu(){}
    
    public NavigationMenu(List<MenuItem> resourceMenuItems,
            List<MenuItem> analyzeMenuItems,
            List<MenuItem> favoriteResourceItems,
            List<MenuItem> recentResourceItems) {
        this.resourceMenuItems = resourceMenuItems;
        this.analyzeMenuItems = analyzeMenuItems;
        this.favoriteResourceItems = favoriteResourceItems;
        this.recentResourceItems = recentResourceItems;
    }

    public List<MenuItem> getResourceMenuItems() {
        return resourceMenuItems;
    }

    public void setResourceMenuItems(List<MenuItem> resourceMenuItems) {
        this.resourceMenuItems = resourceMenuItems;
    }

    public List<MenuItem> getAnalyzeMenuItems() {
        return analyzeMenuItems;
    }

    public void setAnalyzeMenuItems(List<MenuItem> analyzeMenuItems) {
        this.analyzeMenuItems = analyzeMenuItems;
    }

    public List<MenuItem> getFavoriteResourceItems() {
        return favoriteResourceItems;
    }

    public void setFavoriteResourceItems(List<MenuItem> favoriteResourceItems) {
        this.favoriteResourceItems = favoriteResourceItems;
    }

    public List<MenuItem> getRecentResourceItems() {
        return recentResourceItems;
    }

    public void setRecentResourceItems(List<MenuItem> recentResourceItems) {
        this.recentResourceItems = recentResourceItems;
    }
}
