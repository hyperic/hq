/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hyperic.hq.plugin.rabbitmq.objs;

import com.ericsson.otp.erlang.OtpErlangAtom;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangString;

/**
 *
 * @author administrator
 */
public class Application {
private String name,description,version;

    public Application(OtpErlangObject name, OtpErlangObject description, OtpErlangObject version) {
        this.name = ((OtpErlangAtom)name).atomValue();
        this.description = ((OtpErlangString)description).stringValue();
        this.version = ((OtpErlangString)version).stringValue();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "['"+name+"','"+description+"','"+version+"']";
    }

}
