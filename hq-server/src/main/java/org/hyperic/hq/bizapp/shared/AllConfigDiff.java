package org.hyperic.hq.bizapp.shared;

public class AllConfigDiff {
    protected AllConfigResponses newAllConf;
    protected AllConfigResponses deletedAllConf;
    protected AllConfigResponses changedAllConf;
    public AllConfigDiff(AllConfigResponses allNewConfigResponses, AllConfigResponses allChangedConfigResponses,
            AllConfigResponses allDeletedConfigResponses) {
        this.newAllConf=allNewConfigResponses;
        this.changedAllConf=allChangedConfigResponses;
        this.deletedAllConf=allDeletedConfigResponses;
    }
    public AllConfigResponses getNewAllConf() {
        return newAllConf;
    }
    public AllConfigResponses getDeletedAllConf() {
        return deletedAllConf;
    }
    public AllConfigResponses getChangedAllConf() {
        return changedAllConf;
    }
}
