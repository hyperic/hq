package org.hyperic.hq.operation.rabbit.shared;

import org.hyperic.hq.operation.OperationService;
import org.hyperic.hq.operation.rabbit.core.AnnotatedRabbitOperationService;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Helena Edelson
 */
public class OperationServiceTests {

    private OperationService operationService;

    @Before
    public void prepare() {
        //mock connection
        this.operationService = new AnnotatedRabbitOperationService();
    }

    @Test
    public void perform() {
        /*  
        when(this.converter.write(context)).thenReturn("");
        this.operationService.perform("test.operation.name", "0", context);*/
    }
}
