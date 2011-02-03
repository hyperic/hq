package org.hyperic.hq.appdef.server.session;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefResourceTypeValue;

public class ApplicationType
    extends AppdefResourceType {

    @Override
    public int getAppdefType() {
        return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
    }

    @Override
    public AppdefResourceTypeValue getAppdefResourceTypeValue() {
        return new AppdefResourceTypeValue() {
            public int getAppdefType() {
                return AppdefEntityConstants.APPDEF_TYPE_APPLICATION;
            }

            public Long getCTime() {
                return new Long(ApplicationType.this.getCreationTime());
            }

            public String getDescription() {
                return ApplicationType.this.getDescription();
            }

            public Integer getId() {
                return ApplicationType.this.getId();
            }

            public Long getMTime() {
                return new Long(ApplicationType.this.getModifiedTime());
            }

            public String getName() {
                return ApplicationType.this.getName();
            }

            public void setDescription(String desc) {
            }

            public void setId(Integer id) {
            }

            public void setName(String name) {
            }
        };
    }

}
