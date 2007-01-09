package org.hyperic.hq.escalation.server.session;

/**
 * This is a utility class meant to provide some of the basic pieces needed
 * for things to implement {@link Escalatable} 
 */
public abstract class EscalatableBase 
    implements Escalatable
{
    private PerformsEscalations _def;
    private Integer             _id;
    private String              _shortReason;
    private String              _longReason;
    
    protected EscalatableBase(PerformsEscalations def, Integer id,
                              String shortReason, String longReason) 
    {
        _def         = def;
        _id          = id;
        _shortReason = shortReason;
        _longReason  = longReason;
    }
    
    public PerformsEscalations getDefinition() {
        return _def;
    }

    public Integer getId() {
        return _id;
    }

    public String getLongReason() {
        return _longReason;
    }

    public String getShortReason() {
        return _shortReason;
    }
}
