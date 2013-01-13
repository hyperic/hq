package org.hyperic.hq.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.authentication.encoding.*;

public class Md5PlusShaPasswordEncoder implements PasswordEncoder {
    
    private static final Log log = LogFactory.getLog(Md5PlusShaPasswordEncoder.class);
    
    private Md5PasswordEncoder md5PwdEncoder;
    private ShaPasswordEncoder shaPwdEncoder;
    
    public Md5PlusShaPasswordEncoder(Md5PasswordEncoder md5PwdEncoder, ShaPasswordEncoder shaPwdEncoder){
        this.md5PwdEncoder = md5PwdEncoder;
        this.shaPwdEncoder = shaPwdEncoder;
    }
    
    
    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        String md5Encoded = md5PwdEncoder.encodePassword(rawPass, salt);
        String res = shaPwdEncoder.encodePassword(md5Encoded, salt);
        log.debug("encodePassword? " + rawPass + " -> " + res);
        return res;
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws DataAccessException {
        String md5Encoded = md5PwdEncoder.encodePassword(rawPass, salt);
        boolean res = shaPwdEncoder.isPasswordValid(encPass, md5Encoded, salt);
        log.debug("isPasswordValid? " + rawPass + " -> " + encPass + " : " + res);
        // we cannot know for sure if this was md5 encoded as well.
        // We may want to add a signature "ms" at the end of the encoded string, and perform the check on the string without its postfix.
        return res;
    }

}
