package org.hyperic.hq.authz.server.session;

import org.hyperic.hq.bizapp.server.session.ResourceCleanupEventListener;
import org.hyperic.hq.common.VetoException;

public interface ResourceRemover {

    /**
     * Removes edges involving the specified {@link Resource} as either the "to"
     * or "from"
     * @param resource The resource
     * @param relation The relationship type to remove
     */
    void removeEdges(Resource resource, ResourceRelation relation);

    /**
     * Completely removes a {@link Resource}
     * @param subject The user executing the delete
     * @param resource The {@link Resource} to delete
     * @throws VetoException
     */
    void removeResource(AuthzSubject subject, Resource resource) throws VetoException;

    /**
     * Either completely removes a Resource (if nullResourceType is false) or
     * marks a Resource for deletion by nulling out its resourceType. If the
     * Resource type is nulled, also deletes all {@link ResourceEdge}s involving
     * the Resource
     * @param subject The user executing the delete
     * @param resource The {@link Resource} to delete or mark for deletion
     * @param nullResourceType true if Resource is to be marked for deletion
     *        later by {@link ResourceCleanupEventListener}, false if delete now
     */
    void removeResource(AuthzSubject subject, Resource resource, boolean nullResourceType);

}
