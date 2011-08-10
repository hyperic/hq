package org.hyperic.hq.product;

import java.util.LinkedList;
import java.util.List;

public class MonitoredFolderConfig implements IMonitorConfig{
    private String path;
    private String filter;
    private boolean recursive; 
    private List<MonitoredFolderConfig> subFolders;
    
    public MonitoredFolderConfig(String path, String filter, boolean recursive) {
        super();
        this.path = path;
        this.filter = filter;
        this.recursive = recursive;
    }
    
    public String getPath() {
        return path;
    }
    public void setPath(String path) {
        this.path = path;
    }
    public String getFilter() {
        return filter;
    }
    public void setFilter(String filter) {
        this.filter = filter;
    }
    public boolean isRecursive() {
        return recursive;
    }
    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
 
    public String dumpXML(){
        final StringBuffer sb = new StringBuffer();
        sb.append("<folder recursive='" + (recursive ? "true":"false") + "' path='"+getPath()+"' filter='"+getFilter());
        if (subFolders == null || subFolders.size() <= 0){
            sb.append("' />");
            return sb.toString();
        }
        sb.append("'>");
        for (final MonitoredFolderConfig config: subFolders){
            sb.append(config.dumpXML());
        }
        sb.append("<folder>");
        return sb.toString();
    }
    
    @Override
    public String toString(){
        return getPath()+";"+isRecursive()+";"+getFilter()+";";
    }

    
    public List<MonitoredFolderConfig> getSubFolders() {
        return subFolders;
    }

    public void setSubFolders(List<MonitoredFolderConfig> subFolders) {
        this.subFolders = subFolders;
    }

    public void addSubFolder(MonitoredFolderConfig subFolder) {
        if (subFolders == null)
            subFolders = new LinkedList<MonitoredFolderConfig>();
        subFolders.add(subFolder);
    }
}
