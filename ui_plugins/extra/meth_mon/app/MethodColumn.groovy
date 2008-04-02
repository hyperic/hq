import org.hyperic.hibernate.SortField

class MethodColumn implements SortField {
    private String  desc
    private String  value
    private boolean sortable
    
    MethodColumn(desc, value, sortable) {
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
