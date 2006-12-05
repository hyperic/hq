package org.hyperic.hq.galerts.server.session;

import org.hyperic.util.HypericEnum;

/**
 * An alert definition is divided into partitions.  Currently the only two
 * partitions are:
 *
 *  Normal   - Regular style alert definition.  If X happens, el-kabong
 *  Recovery - A partition indicating that an alertable condition is no longer
 *             valid
 */
public class GalertDefPartition 
    extends HypericEnum
{
    // This is just a regular alert definition.  
    public static final GalertDefPartition NORMAL = 
        new GalertDefPartition(0, "NORMAL");
    
    // This alert definition executed a recovery alert
    public static final GalertDefPartition RECOVERY = 
        new GalertDefPartition(1, "RECOVERY");
    
    private GalertDefPartition(int code, String desc) {
        super(code, desc);
    }
    
    public static GalertDefPartition findByCode(int code) {
        return (GalertDefPartition)
            HypericEnum.findByCode(GalertDefPartition.class, code);
    }
}
