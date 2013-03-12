package org.hyperic.hq.vm;

public class VCConnection {
     
    private String url;
    private String user;
    private String password;
    private int index;
    private boolean lastSyncSucceeded = false;
    
    public VCConnection(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }
    
    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean lastSyncSucceeded() {
        return lastSyncSucceeded;
    }

    public void setLastSyncSucceeded(boolean hasConnectionProblem) {
        this.lastSyncSucceeded = hasConnectionProblem;
    }
    
    

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        VCConnection other = (VCConnection) obj;
        if(url == null) {
            if(other.url != null) return false;
        }else if(!url.equals(other.url)) return false;
        return true;
    }

    @Override
    public String toString() {
        return "VCConnection [url=" + url + ", user=" + user + ", password=" + password + ", index=" + index
                + ", lastSyncSucceeded=" + lastSyncSucceeded + "]";
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }


}
