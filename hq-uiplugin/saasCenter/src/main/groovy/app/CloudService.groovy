/**
 * A CloudService houses information which is specific to the rendering & 
 * usage of a specific service type within the HQ backend.
 */
class CloudService {
    String  code
    String  longName
    Boolean sortAsc = true
    Boolean staggerCharts = false
    List<PerformanceMetric> performanceMetrics
    Boolean hasHealth = true
    
    public String toString() {
        longName + " (" + code + ")"
    }
 
    
    boolean equals(other) {
        if (other?.is(this))
            return true
        
        if (!(other instanceof CloudService))
            return false
            
        return code == other.code
    }
    
    int hashCode() {
        code.hashCode()
    }
}