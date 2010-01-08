package org.hyperic.hq.bizapp.server.session;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

public class EmailPasswordAuthenticator extends Authenticator {
    private String username;
    private String password;
    
    public EmailPasswordAuthenticator(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(username, password);
    }
}