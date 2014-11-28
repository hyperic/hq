package com.vmware.hyperic.model.relations;

public class ObjectFactory {

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
     * com.vmware.hyperic.relation.model
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link Resource }
     * 
     */
    public Resource createResource() {
        return new Resource();
    }

    /**
     * Create an instance of {@link Relation }
     * 
     */
    public Relation createRelation() {
        return new Relation();
    }

    /**
     * Create an instance of {@link Identifier }
     * 
     */
    public Identifier createIdentifier() {
        return new Identifier();
    }

    /**
     * Create an instance of the model}
     * 
     */
    public CommonModel createRelationshipModel(CommonModel model) {
        CommonModel commonModel;
        if (model == null) {
            commonModel = new CommonModel();
        } else {
            commonModel = model;
        }

        return commonModel;
    }
}
