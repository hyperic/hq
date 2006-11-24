package org.hyperic.hq.application.test;

import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.application.TransactionListener;
import org.hyperic.hq.application.shared.TestManagerLocal;
import org.hyperic.hq.application.shared.TestManagerUtil;
import org.hyperic.hq.test.HQEJBTestBase;

public class TxCallbackTest 
    extends HQEJBTestBase
{
    private boolean _afterCalled;
    
    public TxCallbackTest(String string) {
        super(string);
    }

    public class MyTxListener implements TransactionListener {
        public void afterCommit() {
            new Throwable().printStackTrace();
            _afterCalled = true;
        }
    }
    
    public void testCommitListeners()
        throws Exception
    {
        final HQApp app = HQApp.getInstance();
        
        _afterCalled  = false;
        
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                app.addTransactionListener(new MyTxListener());
            }
        });

        assertTrue(_afterCalled);
        
        _afterCalled = false;
        try {
            runInTransaction(new TransactionBlock() {
                public void run() throws Exception {
                    app.addTransactionListener(new MyTxListener());
                    throw new Exception();
                }
            });
            fail("Tx should have thrown an exception");
        } catch(Exception e) {
            // Correct;
        }
        
        assertFalse(_afterCalled);
        
        // Now check to make sure it works with container methods
        final TestManagerLocal tl = TestManagerUtil.getLocalHome().create();

        _afterCalled = false;
        try {
            runInTransaction(new TransactionBlock() {
               public void run() throws Exception {
                   app.addTransactionListener(new MyTxListener());
                   tl.throwException();
               }
            });
            fail("Tx should have thrown an exception");
        } catch(Exception e) {
            // Correct
        }
        assertFalse(_afterCalled);
        
        _afterCalled = false;
        runInTransaction(new TransactionBlock() {
            public void run() throws Exception {
                app.addTransactionListener(new MyTxListener());
                tl.noop();
            }
        });
        assertTrue(_afterCalled);
    }
}
