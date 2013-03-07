package org.hyperic.hq.vm;

public class VCCredentials {
     
    private String url;
    private String user;
    private String password;
    private boolean lastSyncSucceeded = false;
    
    public VCCredentials(String url, String user, String password) {
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
        result = prime * result + ((password == null) ? 0 : password.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
        result = prime * result + ((user == null) ? 0 : user.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj) return true;
        if(obj == null) return false;
        if(getClass() != obj.getClass()) return false;
        VCCredentials other = (VCCredentials) obj;
        if(password == null) {
            if(other.password != null) return false;
        }else if(!password.equals(other.password)) return false;
        if(url == null) {
            if(other.url != null) return false;
        }else if(!url.equals(other.url)) return false;
        if(user == null) {
            if(other.user != null) return false;
        }else if(!user.equals(other.user)) return false;
        return true;
    }
    
    @Override
    public String toString() {
        return "VCCredentials [url=" + url + ", user=" + user + ", password=" + password + "]";
    }


}
