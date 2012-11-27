package org.hyperic.hq.security;

import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.encoding.*;

public class Md5PlusShaPasswordEncoder implements PasswordEncoder {
    
    Md5PasswordEncoder md5PwdEncoder;
    ShaPasswordEncoder shaPwdEncoder;

    public Md5PlusShaPasswordEncoder() {
        //super("Md5PlusSha");
        md5PwdEncoder = new Md5PasswordEncoder();
        shaPwdEncoder = new ShaPasswordEncoder(256);
    }

    
    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        String md5Encoded = md5PwdEncoder.encodePassword(rawPass, salt);
        String res = shaPwdEncoder.encodePassword(md5Encoded, salt);
        return res;
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws DataAccessException {
        boolean res = shaPwdEncoder.isPasswordValid(encPass, rawPass, salt);
        // we cannot know for sure if this was md5 encoded as well.
        // We may want to add a signature "ms" at the end of the encoded string, and perform the check on the string without its postfix.
        return res;
    }

}
