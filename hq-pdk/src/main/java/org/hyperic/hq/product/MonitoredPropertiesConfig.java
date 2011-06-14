package org.hyperic.hq.product;

import java.util.List;

public class MonitoredPropertiesConfig implements IMonitorConfig{

    private String type;
    private String path;
    private List<MonitoredFolderConfig> folders;
    
    public MonitoredPropertiesConfig(String type, String path) {
        super();
        this.type = type;
        this.path = path;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    
    public List<MonitoredFolderConfig> getFolders() {
        return folders;
    }

    public void setFolders(List<MonitoredFolderConfig> folders) {
        this.folders = folders;
    }

    public String dumpXML() {
        final StringBuffer sb = new StringBuffer();
        final String elName = getType();
        sb.append("<"+elName+" path='"+getPath());
        if (folders == null || folders.size() <= 0){
            sb.append("' />");
            return sb.toString();
        }
        sb.append("'>");
        for (final MonitoredFolderConfig config: folders){
            sb.append(config.dumpXML());
        }
        sb.append("<"+elName+">");
        return sb.toString();
    }

    @Override
    public String toString(){
        return getPath()+";"+getType()+";";
    }

}
