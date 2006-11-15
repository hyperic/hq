package org.hyperic.hq.control.shared;

import java.io.Serializable;

/**
 * Simple data object for returning data for the Dashboard
 */
public class ControlFrequencyValue implements Serializable {

    private String name;
    private String action;
    private int    id;
    private int    type;
    private int    num;

    public ControlFrequencyValue(String name, int type, int id,
                                 String action, int num) {
        this.name = name;
        this.action = action;
        this.type = type;
        this.id = id;
        this.num = num;
    }

    // Getters
    public String getName() {
        return this.name;
    }

    public String getAction() {
        return this.action;
    }

    public int getNum() {
        return this.num;
    }

    public int getId() {
        return this.id;
    }

    public int getType() {
        return this.type;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setNum(int num) {
        this.num = num;
    }
}

    
