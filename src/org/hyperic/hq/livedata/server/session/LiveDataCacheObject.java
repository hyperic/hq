package org.hyperic.hq.livedata.server.session;

import org.hyperic.hq.livedata.shared.LiveDataResult;

class LiveDataCacheObject {

    private LiveDataResult[] _result;
    private long _ctime;

    public LiveDataCacheObject(LiveDataResult[] res) {
        _result = res;
        _ctime = System.currentTimeMillis();
    }

    public LiveDataResult[] getResult() {
        return _result;
    }

    public long getCtime() {
        return _ctime;
    }
}
