import org.hyperic.hibernate.SortField

class CacheColumn implements SortField {
    private String  desc
    private String  value
    private boolean sortable
    
    CacheColumn(desc, value, sortable) {
    	this.desc     = desc
    	this.value    = value
    	this.sortable = sortable
    }
    
    String getDescription() {
        desc
    }
    
    String getValue() {
        value
    }
    
    boolean isSortable() {
        sortable
    }
}
