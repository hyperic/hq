package org.hyperic.hq.bizapp.client.pageFetcher;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.bizapp.client.pageFetcher.GenericEJBFetcher;
import org.hyperic.hq.bizapp.shared.ControlBoss;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageFetchException;
import org.hyperic.util.pager.PageList;

public class FindControlJobFetcher
    extends GenericEJBFetcher
{
    private AppdefEntityID id;
    private ControlBoss    boss;
    private int            authToken;

    public FindControlJobFetcher(ControlBoss boss, int authToken,
                                 AppdefEntityID id){
        this.boss      = boss;
        this.authToken = authToken;
        this.id        = id;
    }

    public PageList getEJBPage(PageControl control)
        throws PageFetchException
    {
        try {
            return this.boss.findJobHistory(this.authToken, this.id, control);
        } catch(Exception exc){
            throw new PageFetchException(exc);
        }
    }
}
