/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss7.objects;

/**
 *
 * @author administrator
 */
public class Deployment {

    private Result result;

    @Override
    public String toString() {
        return "Deployment{" + "result=" + result + '}';
    }

    public String getName() {
        return result.name;
    }

    public String getRuntimeName() {
        return result.runtimeName;
    }

    public Boolean getEnabled() {
        return result.enabled;
    }

    static class Result {

        private String name;
        private String runtimeName;
        private Boolean enabled;

        @Override
        public String toString() {
            return "name=" + name + ", runtimeName=" + runtimeName + ", enabled=" + enabled;
        }
    }
}
