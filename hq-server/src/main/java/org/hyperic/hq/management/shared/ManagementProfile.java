package org.hyperic.hq.management.shared;

import java.util.Collection;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.server.session.Crispo;

@SuppressWarnings("serial")
public class ManagementProfile extends PersistedObject {
    
    private String description;
    private Resource resource;
    private Crispo config;
    private Collection<MeasurementInstruction> measurementInstructions;

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
    public Collection<MeasurementInstruction> getMeasurementInstructions() {
        return measurementInstructions;
    }
    public void setMeasurementInstructions(
        Collection<MeasurementInstruction> measurementInstructions) {
        this.measurementInstructions = measurementInstructions;
    }

}
