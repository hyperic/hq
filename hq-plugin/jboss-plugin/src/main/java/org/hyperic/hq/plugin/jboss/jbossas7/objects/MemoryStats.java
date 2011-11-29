/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss.jbossas7.objects;

/**
 *
 * @author administrator
 */
public class MemoryStats {

    private String init;
    private String used;
    private String committed;
    private String max;

    /**
     * @return the init
     */
    public String getInit() {
        return init;
    }

    /**
     * @param init the init to set
     */
    public void setInit(String init) {
        this.init = init;
    }

    /**
     * @return the used
     */
    public String getUsed() {
        return used;
    }

    /**
     * @param used the used to set
     */
    public void setUsed(String used) {
        this.used = used;
    }

    /**
     * @return the committed
     */
    public String getCommitted() {
        return committed;
    }

    /**
     * @param committed the committed to set
     */
    public void setCommitted(String committed) {
        this.committed = committed;
    }

    /**
     * @return the max
     */
    public String getMax() {
        return max;
    }

    /**
     * @param max the max to set
     */
    public void setMax(String max) {
        this.max = max;
    }

    /**
    http://docs.oracle.com/javase/1.5.0/docs/api/index.html?java/lang/management/ThreadMXBean.html
    +----------------------------------------------+
    +////////////////           |                  +
    +////////////////           |                  +
    +----------------------------------------------+
    
    |--------|
    init
    |---------------|
    used
    |---------------------------|
    committed 
    |----------------------------------------------|
    max   
     */
    public String getUsedPercentage() {
        double p = Double.parseDouble(used) / Double.parseDouble(committed);
        return Double.toString(p);
    }

    @Override
    public String toString() {
        return "MemoryStats{" + "init=" + init + ", used=" + used + ", committed=" + committed + ", max=" + max + '}';
    }
}
