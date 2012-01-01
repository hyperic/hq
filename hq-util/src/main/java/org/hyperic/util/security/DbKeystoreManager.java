package org.hyperic.util.security;

import java.security.cert.Certificate;
import java.util.Collection;

public interface DbKeystoreManager {

    public Collection<KeystoreEntry> getKeystore();

    public void create(String alias, String type, Certificate cert, Certificate[] chain);

}
