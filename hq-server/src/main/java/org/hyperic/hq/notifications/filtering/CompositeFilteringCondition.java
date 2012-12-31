package org.hyperic.hq.notifications.filtering;

public abstract class CompositeFilteringCondition<E> extends FilteringCondition<E> {
    protected Operand operand;
    protected FilteringCondition<E> leftFilteringCondition;
    protected FilteringCondition<E> rightFilteringCondition;
    
    enum Operand {
        And(),
        Or();
    }
}
