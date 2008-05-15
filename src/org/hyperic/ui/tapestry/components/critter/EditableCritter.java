package org.hyperic.ui.tapestry.components.critter;

import java.io.Serializable;
import java.util.Map;

import org.hyperic.hq.grouping.CritterType;

public class EditableCritter 
    implements Serializable
{
    private int         _idx;
    private CritterType _type;
    private Map         _props;
    private String      _config;
    
    public EditableCritter(EditableCritter o) {
        _idx    = o._idx;
        _type   = o._type;
        _props  = o._props;
        _config = o._config;
    }
    
    public EditableCritter(int idx, CritterType type, Map props) {
        _idx        = idx;
        _type       = type;
        _props      = props;
        _config     = null;
    }

    public int getIndex() {
        return _idx;
    }

    public String getConfig() {
        return _config;
    }

    public void setConfig(String config) {
        _config = config;
    }

    public CritterType getType() {
        return _type;
    }

    public void setType(CritterType t) {
        _type = t;
    }

    public Map getProps() {
        return _props;
    }

    public void setProps(Map props) {
        _props = props;
    }
}
