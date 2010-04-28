/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss;

/**
 *
 * @author laullon
 */
public class JBoss5ServiceControlPlugin extends JBossStateServiceControlPlugin {

    void start() {
        invokeMethod("start");
        setResult(getResult());
    }

    void stop() {
        invokeMethod("stop");
        setResult(getResult());
    }
}
