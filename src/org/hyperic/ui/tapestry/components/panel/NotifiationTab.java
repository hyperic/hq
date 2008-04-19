package org.hyperic.ui.tapestry.components.panel;

public class NotifiationTab {

    private String _title;
    private String _name;
    private String _icon;
    private int _size;

    public NotifiationTab(String _title, String _name, String _icon, int _size) {
        super();
        this._title = _title;
        this._name = _name;
        this._icon = _icon;
        this._size = _size;
    }

    public String get_title() {
        return _title;
    }

    public void set_title(String _title) {
        this._title = _title;
    }

    public String get_name() {
        return _name;
    }

    public void set_name(String _name) {
        this._name = _name;
    }

    public String get_icon() {
        return _icon;
    }

    public void set_icon(String _icon) {
        this._icon = _icon;
    }

    public int get_size() {
        return _size;
    }

    public void set_size(int _size) {
        this._size = _size;
    }
}
