// This test verifies that the HQApp is correctly counting the transactions
// (and failed transactions)

import org.hyperic.hq.common.server.session.TransactionManagerEJBImpl as TxMan
import org.hyperic.hq.application.HQApp
import org.hyperic.util.Runnee

def app     = HQApp.instance
def txMan = TxMan.one

def startNumTx = app.transactions
def startFailTx = app.transactionsFailed

try {
    txMan.runInTransaction({run: {
        throw new RuntimeException('Throw an intentional error, so the Tx is aborted')
    }} as Runnee)
    assert false, "Should have thrown an exception"
} catch(Exception e){
}

assert app.transactions > startNumTx
assert app.transactionsFailed > startFailTx

startNumTx = app.transactions
startFailTx   = app.transactionsFailed

assert 1 == txMan.runInTransaction({run: {
    return 1
}} as Runnee)

assert app.transactions > startNumTx
assert app.transactionsFailed == startFailTx // This may fail if something else on the server is blowing up
