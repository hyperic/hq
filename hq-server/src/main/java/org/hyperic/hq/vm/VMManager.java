package org.hyperic.hq.vm;

import java.net.MalformedURLException;
import java.rmi.RemoteException;


public interface VMManager {
    public void collect(final String url, final String usr, final String pass) throws RemoteException, MalformedURLException;
}
