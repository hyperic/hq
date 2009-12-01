package org.hyperic.ui.tapestry.components.critter;

import java.io.Serializable;

import org.hyperic.hq.grouping.prop.CritterPropType;

public class EditableCritterProp 
    implements Serializable
{
    private String          _id;
    private String          _name;
    private String          _purpose;
    private CritterPropType _type;
    private String          _stringValue;

    public EditableCritterProp(EditableCritterProp orig) {
        _id          = orig._id;
        _name        = orig._name;
        _purpose     = orig._purpose;
        _type        = orig._type;
        _stringValue = orig._stringValue;
    }
    
    public EditableCritterProp(String id, String name, String purpose,
                               CritterPropType type, String stringValue)
    {
        _id          = id;
        _name        = name;
        _purpose     = purpose;
        _type        = type;
        _stringValue = stringValue;
    }

    public String getId() {
        return _id;
    }

    public String getName() {
        return _name;
    }

    public String getPurpose() {
        return _purpose;
    }

    public CritterPropType getType() {
        return _type;
    }

    public String getStringValue() {
        return _stringValue;
    }

    public void setStringValue(String s) {
        System.out.println("Changing value of property [" + _id + "] to [" + 
                           s + "] from [" + _stringValue + "]");
        _stringValue = s;
    }

    public boolean isStringProp() {
        return _type.equals(CritterPropType.STRING);
    }
    /*
        Date getDateValue();
        HypericEnum getEnumValue();
        int getResourceId();
        int getProtoId();
        int getGroupId();
        int getSubjectId();
     */
}