package org.hyperic.tools.ant;

import org.apache.tools.ant.types.DataType;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.ShaPasswordEncoder;

public class EncryptProperty extends DataType {


    private String property;
    private String targetProperty;

    public String getTargetProperty() {
        return targetProperty;
    }

    public void setTargetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
    }

    private int strength = 256;//set 256 as default just in case.
    private boolean encodeHashAsBase64;

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public int getStrength() {
        return strength;
    }

    public void setStrength(int strength) {
        this.strength = strength;
    }
    
    public boolean isEncodeHashAsBase64() {
        return encodeHashAsBase64;
    }

    public void setEncodeHashAsBase64(boolean encodeHashAsBase64) {
        this.encodeHashAsBase64 = encodeHashAsBase64;
    }

    public String encode(String value){
        // TODO: the following code copies the Md5PlusShaPasswordEncoder, which resides in a dependant project.
        // This class should be moved into this project instead.
        Md5PasswordEncoder md5PwdEncoder = new Md5PasswordEncoder();
        md5PwdEncoder.setEncodeHashAsBase64(encodeHashAsBase64 );
        ShaPasswordEncoder shaPwdEncoder = new ShaPasswordEncoder(strength);
        shaPwdEncoder.setEncodeHashAsBase64(encodeHashAsBase64 );
    
        String md5Encoded = md5PwdEncoder.encodePassword(value, null);
         String encrypted = shaPwdEncoder.encodePassword(md5Encoded, null);
         return encrypted;
    }
  

}
