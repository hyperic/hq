package org.hyperic.hq.management.shared;

import java.util.Collection;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.server.session.Crispo;

@SuppressWarnings("serial")
public class ManagementPolicy extends PersistedObject {
    
    private String description;
    private Resource resource;
    private Crispo config;
    private Collection<MeasurementInstruction> measurementInstructionBag;

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Resource getResource() {
        return resource;
    }
    public void setResource(Resource resource) {
        this.resource = resource;
    }
    public Crispo getConfig() {
        return config;
    }
    public void setConfig(Crispo config) {
        this.config = config;
    }
    public Collection<MeasurementInstruction> getMeasurementInstructionBag() {
        return measurementInstructionBag;
    }
    public void setMeasurementInstructionBag(Collection<MeasurementInstruction> measurementInstructions) {
        this.measurementInstructionBag = measurementInstructions;
    }
    
    public String getName() {
        if (resource != null)
            return resource.getName();
        return "";
    }

    public void setName(String name) {
        if (resource != null)
            resource.setName(name);
    }

    public long getModifiedDate() {
        if (resource != null)
            return resource.getMtime();
        return -1;// marker for bad date
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("resource=").append(resource).append(",name=").append(resource.getName())
            .append(",description=").append(description)
            .append(",prototype=").append(resource.getPrototype().getName())
// XXX need to eventually take this out, or *** out secret options
.append(",config:").append(config)
            .toString();
    }

}
