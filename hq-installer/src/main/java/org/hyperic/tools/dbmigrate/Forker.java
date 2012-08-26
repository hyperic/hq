package org.hyperic.tools.dbmigrate;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hyperic.util.MultiRuntimeException;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader.Array;

public class Forker {

    public static final <V, T extends Callable<V[]>> List<Future<V[]>> fork(final int bufferSize, final int maxWorkers,
            final ForkContext<V, T> context) throws Throwable {

        ExecutorService executorPool = null;
        try{
            int iNoOfWorkers = bufferSize < maxWorkers ? bufferSize : maxWorkers;

            final List<Future<V[]>> workersResponses = new ArrayList<Future<V[]>>(iNoOfWorkers);
            executorPool = Executors.newFixedThreadPool(iNoOfWorkers);
            context.inverseSemaphore = new CountDownLatch(iNoOfWorkers);

            Future<V[]> workerResponse = null;
            Callable<V[]> worker = null;
            for (int i = 0; i < iNoOfWorkers; i++) {
                worker = context.workerFactory.newWorker(context);
                workerResponse = executorPool.submit(worker);
                workersResponses.add(workerResponse);
            }// EO while there are more workers

            context.inverseSemaphore.await();

            return workersResponses;
        }catch (Throwable t) {
            Utils.printStackTrace(t);
            throw t;
        }finally {
            if (executorPool != null) executorPool.shutdown();
        }//EO catch block
    }// EOM

    public static abstract class ForkWorker<V> implements Callable<V[]> {
        protected final Connection conn;
        private final CountDownLatch countdownSemaphore;
        protected BlockingDeque<V> sink;
        private Class<V> entityType ; 

        protected ForkWorker(final CountDownLatch countdownSemaphore, Connection conn, BlockingDeque<V> sink, final Class<V> clsEntityType) {
            this.countdownSemaphore = countdownSemaphore;
            this.conn = conn;
            this.sink = sink;
            this.entityType = clsEntityType ; 
        }//EOM

        protected abstract void callInner(final V entity) throws Throwable;

        @SuppressWarnings("unchecked")
        public V[] call() throws Exception {
            final List<V> processedEntities = new ArrayList<V>();
            MultiRuntimeException thrown = null;
            
            try{
                V entity = null;
                Throwable innerException = null;

                while ((entity = this.sink.poll()) != null){
                    try {
                        processedEntities.add(entity);
                        callInner(entity);
                        
                    }catch (Throwable t2) {
                        Utils.printStackTrace(t2);
                        innerException = t2;
                    }finally {
                        try {
                            if(innerException == null) {
                                this.conn.commit();
                            }else {
                                rollbackEntity(entity, innerException);
                                innerException = null;
                                this.conn.rollback();
                            }//EO else if there was an error 
                        }catch (Throwable t2) {
                            Utils.printStackTrace(t2);
                            MultiRuntimeException.newMultiRuntimeException(thrown, t2);
                        }//EO inner catch block

                        if (this.countdownSemaphore != null) this.countdownSemaphore.countDown();
                    }//EO catch block 
                }//EO while there are more entities 
            } catch (Throwable t1) {
                Utils.printStackTrace(t1);
                MultiRuntimeException.newMultiRuntimeException(thrown, t1);
            } finally {
                try {
                    Utils.close(thrown != null ? Utils.ROLLBACK_INSTRUCTION_FLAG : Utils.NOOP_INSTRUCTION_FLAG, new Object[] { this.conn });
                } catch (Throwable t1) {
                    MultiRuntimeException.newMultiRuntimeException(thrown, t1);
                }//EO inner catch block 
 
                if (thrown != null) throw thrown;
            }//EO catch block 

            final V[] arrResponse = (V[]) java.lang.reflect.Array.newInstance(this.entityType, processedEntities.size()) ; 
            return processedEntities.toArray(arrResponse);
        }//EOM 

        protected final void rollbackEntity(final Object entity, final Throwable t) { /*NOOP*/}//EOM
    }//EOM 

    public static abstract interface WorkerFactory<V, T extends Callable<V[]>> {
        T newWorker(final Forker.ForkContext<V, T> paramForkContext) throws Throwable;
    }//EO interface WorkerFactory 

    @SuppressWarnings({"rawtypes"})
    public static class ForkContext<V, T extends Callable<V[]>> extends HashMap<Object,Object> {

        private static final long serialVersionUID = -1221616165268097942L;
        
        private BlockingDeque<V> sink;
        private Forker.WorkerFactory<V, T> workerFactory;
        private CountDownLatch inverseSemaphore;
        private Hashtable env;

        public ForkContext(BlockingDeque<V> sink, Forker.WorkerFactory<V, T> workerFactory, Hashtable env) {
            this.sink = sink;
            this.workerFactory = workerFactory;
            this.env = env;
        }//EOM

        public final Hashtable getEnv() {
            return this.env;
        }//EOM

        public final CountDownLatch getSemaphore() {
            return this.inverseSemaphore;
        }//EOM

        public final BlockingDeque<V> getSink() {
            return this.sink;
        }//EOM
    }//EO inner class ForkContext
    
}//EOC
