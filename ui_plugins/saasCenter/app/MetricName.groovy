class MetricName implements Comparable {
    private final String _protoName 
    private final String _metricName
    
    MetricName(String protoName, String metricName) {
        _protoName  = protoName
        _metricName = metricName
    }
    
    public int hashCode() {
        return _protoName.hashCode() + _metricName.hashCode()
    }
    
    public boolean equals(Object rhs) {
        if (!(rhs instanceof MetricName)) {
            return false
        } else if (_protoName.equals(rhs.protoName) &&
                   _metricName.equals(rhs.metricName)) {
            return true
        }
        return false
    }
    
    public String getMetricName() {
        return _metricName
    }
    
    public String getProtoName() {
        return _protoName
    }
    
    public String toString() {
        return "${_protoName} ${_metricName}"
    }
    
    public int compareTo(Object rhs) throws ClassCastException {
        if (!(rhs instanceof MetricName)) {
            throw new ClassCastException()
        }
        def tmp
        if (0 != (tmp = _metricName.compareTo(rhs.metricName))) {
            return tmp
        }
        return _protoName.compareTo(rhs.protoName)
    }
}
