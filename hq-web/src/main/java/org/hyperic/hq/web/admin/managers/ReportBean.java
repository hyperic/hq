package org.hyperic.hq.web.admin.managers;

import java.util.List;

public class ReportBean {

        private int id;
        private String fqdn;
        private String ip;
        private String osType;
        private List<String> listOfPlugins;
        public int getId() {
                return id;
        }
        public void setId(int id) {
                this.id = id;
        }
        public String getFqdn() {
                return fqdn;
        }
        public void setFqdn(String fqdn) {
                this.fqdn = fqdn;
        }
        public String getIp() {
                return ip;
        }
        public void setIp(String ip) {
                this.ip = ip;
        }
        public String getOsType() {
                return osType;
        }
        public void setOsType(String osType) {
                this.osType = osType;
        }
        public List<String> getListOfPlugins() {
                return listOfPlugins;
        }
        public void setListOfPlugins(List<String> listOfPlugins) {
                this.listOfPlugins = listOfPlugins;
        }

}
