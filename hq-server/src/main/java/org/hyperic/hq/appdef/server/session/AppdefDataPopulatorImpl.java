package org.hyperic.hq.appdef.server.session;

import java.util.HashSet;
import java.util.Set;

import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.inventory.data.ResourceDao;
import org.hyperic.hq.inventory.data.ResourceTypeDao;
import org.hyperic.hq.inventory.domain.PropertyType;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.inventory.domain.ResourceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class AppdefDataPopulatorImpl implements AppdefDataPopulator {

    private ResourceDao resourceDao;

    private ResourceTypeDao resourceTypeDao;

    @Autowired
    public AppdefDataPopulatorImpl(ResourceTypeDao resourceTypeDao, ResourceDao resourceDao) {
        this.resourceTypeDao = resourceTypeDao;
        this.resourceDao = resourceDao;
    }

    private void createApplicationResourceType() {
        if (resourceTypeDao.findByName(AppdefEntityConstants.APPDEF_NAME_APPLICATION) == null) {
            ResourceType groupType = new ResourceType(AppdefEntityConstants.APPDEF_NAME_APPLICATION);
            resourceTypeDao.persist(groupType);
            Set<PropertyType> propTypes = new HashSet<PropertyType>();
            propTypes
                .add(createPropertyType(ApplicationManagerImpl.BUSINESS_CONTACT, String.class));
            propTypes.add(createPropertyType(ApplicationManagerImpl.CREATION_TIME, Long.class));
            propTypes.add(createPropertyType(ApplicationManagerImpl.ENG_CONTACT, String.class));
            propTypes.add(createPropertyType(ApplicationManagerImpl.MODIFIED_TIME, Long.class));
            propTypes.add(createPropertyType(ApplicationManagerImpl.OPS_CONTACT, String.class));
            groupType.addPropertyTypes(propTypes);
        }
    }

    private void createGroupResourceTypes() {
        int[] groupTypes = AppdefEntityConstants.getAppdefGroupTypes();
        for (int i = 0; i < groupTypes.length; i++) {
            if (resourceTypeDao.findByName(AppdefEntityConstants
                .getAppdefGroupTypeName(groupTypes[i])) == null) {
                ResourceType groupType = new ResourceType(
                    AppdefEntityConstants.getAppdefGroupTypeName(groupTypes[i]));
                resourceTypeDao.persist(groupType);
                Set<PropertyType> propTypes = new HashSet<PropertyType>();
                propTypes.add(createPropertyType("groupEntType", Integer.class, false));
                propTypes.add(createPropertyType("groupEntResType", Integer.class, true));
                propTypes.add(createPropertyType("mixed", Boolean.class, true));
                groupType.addPropertyTypes(propTypes);
            }
        }
    }

    private void createIpResourceType() {
        if (resourceTypeDao.findByName(PlatformManagerImpl.IP_RESOURCE_TYPE_NAME) == null) {
            ResourceType ipType = new ResourceType(PlatformManagerImpl.IP_RESOURCE_TYPE_NAME);
            resourceTypeDao.persist(ipType);
            // TODO ipType isn't really getting a plugin here.
            // Maybe give it System plugin or consider making it a first class
            // citizen in new model?
            Set<PropertyType> ipPropTypes = new HashSet<PropertyType>();
            ipPropTypes.add(createPropertyType(PlatformFactory.IP_ADDRESS, String.class));
            ipPropTypes.add(createPropertyType(PlatformFactory.NETMASK, String.class));
            ipPropTypes.add(createPropertyType(PlatformFactory.MAC_ADDRESS, String.class));
            ipPropTypes.add(createPropertyType(PlatformFactory.CREATION_TIME, Long.class));
            ipPropTypes.add(createPropertyType(PlatformFactory.MODIFIED_TIME, Long.class));
            ipType.addPropertyTypes(ipPropTypes);
        }
    }

    private PropertyType createPropertyType(String propTypeName, Class<?> type) {
        return createPropertyType(propTypeName, type, false);
    }

    private PropertyType createPropertyType(String propTypeName, Class<?> type, boolean indexed) {
        PropertyType propType = new PropertyType(propTypeName, type);
        propType.setDescription(propTypeName);
        propType.setHidden(true);
        propType.setIndexed(indexed);
        return propType;
    }

    private void createRootResourceAndType() {
        if (resourceTypeDao.findRoot() == null) {
            ResourceType system = new ResourceType("System");
            resourceTypeDao.persistRoot(system);
            Resource root = new Resource("Root", system);
            resourceDao.persistRoot(root);
        }
    }

    public void populateData() {
        createRootResourceAndType();
        createGroupResourceTypes();
        createApplicationResourceType();
        createIpResourceType();
    }
}
