package org.hyperic.hq.appdef.server.session;


import org.hyperic.hq.context.IntegrationTestContextLoader;
import org.hyperic.hq.inventory.domain.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:META-INF/spring/*-context.xml", loader = IntegrationTestContextLoader.class)
public class TxTimingTest {

    @Autowired
    private NodeTester nodeTester;
    
    @Test
    public void testIt() {
        Resource resource = new Resource();
        resource.setName("A");
        resource.persist();
        Thread thread1 = new Thread(new Runnable() {
            public void run() {
                nodeTester.createAndRelate();
            }
        });
        thread1.start();
        Thread thread2 = new Thread(new Runnable() {
            public void run() {
                nodeTester.traverse();
            }
        });
        thread2.start();
        System.out.println("Stop here");
    }
    
}
