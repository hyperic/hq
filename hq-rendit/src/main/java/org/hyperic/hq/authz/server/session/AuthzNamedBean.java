package org.hyperic.hq.authz.server.session;

public abstract class AuthzNamedBean {
    private String _name;
    private String _sortName;
    private Integer id;

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }
    
    protected AuthzNamedBean() {
    }

    protected AuthzNamedBean(String name) {
        _name = name;
        setSortName(name);
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        if (name == null)
            name = "";
        _name = name;
        setSortName(name);
    }

    public String getSortName() {
        return _sortName;
    }

    public void setSortName(String sortName) {
        _sortName = sortName != null ? sortName.toUpperCase() : null;
    }

    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj instanceof AuthzNamedBean == false) {
            return false;
        }

        AuthzNamedBean o = (AuthzNamedBean) obj;
        return ((_name == o.getName()) || (_name != null && o.getName() != null && _name.equals(o
            .getName())));
    }

    public int hashCode() {
        int result = super.hashCode();

        result = 37 * result + (_name != null ? _name.hashCode() : 0);

        return result;
    }

    public class Comparator implements java.util.Comparator {

        public int compare(Object arg0, Object arg1) {
            if (!(arg0 instanceof AuthzNamedBean) || !(arg1 instanceof AuthzNamedBean))
                return 0;

            int compVal = ((AuthzNamedBean) arg0).getName().compareTo(
                ((AuthzNamedBean) arg1).getName());

            // Same name doesn't mean same object
            return compVal == 0 ? 1 : compVal;
        }
    }
}
