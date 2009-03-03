import org.hyperic.hibernate.SortField

public class TableColumn implements SortField {

    private String _desc
    private String _val
    private boolean _sort

    public TableColumn(String desc, String val, boolean sort) {
        _desc = desc
        _val = val
        _sort = sort
    }

    public String getDescription() {
        _desc
    }

    public String getValue() {
        _val
    }

    public boolean isSortable() {
        _sort
    }
}