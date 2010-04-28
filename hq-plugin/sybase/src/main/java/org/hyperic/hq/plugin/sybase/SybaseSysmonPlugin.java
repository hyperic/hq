/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.sybase;

import org.hyperic.hq.product.*;

/**
 *
 * @author laullon
 */
public class SybaseSysmonPlugin extends MeasurementPlugin {

    public Collector getNewCollector() {
        return new SybaseSysmonCollector();
    }
}
