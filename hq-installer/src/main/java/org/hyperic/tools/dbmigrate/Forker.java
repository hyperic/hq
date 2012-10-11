/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
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

/**
 * 
 * Map reduce service class which spawns configurable amount for worker threads to share the load of a task buffer<br/> 
 * and awaits for all workers to finish prior to returning to the caller. 
 * <p>
 * The class delegates the worker instance creation to the {@link WorkerFactory} formal argument instance.
 * </p> 
 */
public class Forker {

    /**
     * Spawns configurable amount of worker threads and awaits until all workers are finished prior to returning to the caller 
     * @param bufferSize - number of elements in the task buffer. Used to configure the number of workers as if the value is<br/>
     *                     smaller than the value of the maxWorkers, the number of workers spawned would be the bufferSize.
     * @param maxWorkers number of threads to initialize unless bigger than the buffer size. 
     * @param context {@link ForkContext} instance containing the {@link WorkerFactory} and the workers' sink 
     * @return a list of {@link Future} each containing an array of entities processed by the 
     * @throws Throwable
     */
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

    /**
     * Base worker class handling the task polling, error handler and connection closure 
     * @author guy
     *
     * @param <V>
     */
    public static abstract class ForkWorker<V> implements Callable<V[]> {
        protected final Connection conn;
        private final CountDownLatch countdownSemaphore;
        private Class<V> entityType ; 
        protected BlockingDeque<V> sink;
        protected MultiRuntimeException accumulatedErrors ;  

        protected ForkWorker(final CountDownLatch countdownSemaphore, 
                Connection conn, BlockingDeque<V> sink, 
                final Class<V> clsEntityType, final MultiRuntimeException accumulatedErrors) {
            this.countdownSemaphore = countdownSemaphore;
            this.conn = conn;
            this.sink = sink;
            this.entityType = clsEntityType ; 
            this.accumulatedErrors = accumulatedErrors ; 
        }//EOM

        protected abstract void callInner(final V entity) throws Throwable;

        @SuppressWarnings("unchecked")
        public V[] call() throws Exception {
            final List<V> processedEntities = new ArrayList<V>();
            MultiRuntimeException thrown = null;
            
            try{
                V entity = null;
                Throwable innerException = null;
                final String errorMsg = "[" + this.getClass().getName() + "] : an Error had occured while processing entity: " ; 
                boolean hadPolledFirst = false ; 
                while ((entity = (hadPolledFirst ? this.sink.pollLast() : this.sink.poll())) != null){
                    try {
                        processedEntities.add(entity);
                        callInner(entity);
                        
                        hadPolledFirst = !hadPolledFirst  ;
                    }catch (Throwable t2) {
                        final String msg = errorMsg + entity ; 
                        this.reportError(t2, msg) ;    
                        Utils.printStackTrace(t2, msg);
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
                            Utils.printStackTrace(t2, errorMsg + entity);
                            MultiRuntimeException.newMultiRuntimeException(thrown, t2);
                        }//EO inner catch block

                    }//EO catch block 
                }//EO while there are more entities 
            } catch (Throwable t1) {
                Utils.printStackTrace(t1);
                MultiRuntimeException.newMultiRuntimeException(thrown, t1);
            } finally {
                /*try {
                    Utils.close(thrown != null ? Utils.ROLLBACK_INSTRUCTION_FLAG : Utils.NOOP_INSTRUCTION_FLAG, new Object[] { this.conn });
                } catch (Throwable t1) {
                    MultiRuntimeException.newMultiRuntimeException(thrown, t1);
                }//EO inner catch block 
*/                
                try {
                    this.dispose(thrown) ;
                } catch (Throwable t1) {
                    MultiRuntimeException.newMultiRuntimeException(thrown, t1);
                }//EO inner catch block 
                
                if (this.countdownSemaphore != null) this.countdownSemaphore.countDown();
                
                if (thrown != null) throw thrown;
            }//EO catch block 

            final V[] arrResponse = (V[]) java.lang.reflect.Array.newInstance(this.entityType, processedEntities.size()) ; 
            return processedEntities.toArray(arrResponse);
        }//EOM 
        
        protected synchronized void reportError(final Throwable throwable, final String errorMsg) { 
            this.accumulatedErrors.addThrowable(throwable, errorMsg) ;
        }//EOM 
        
        protected void dispose(final MultiRuntimeException thrown) throws Throwable { 
            try {
                Utils.close(thrown != null ? Utils.ROLLBACK_INSTRUCTION_FLAG : Utils.NOOP_INSTRUCTION_FLAG, new Object[] { this.conn });
            } catch (Throwable t1) {
                MultiRuntimeException.newMultiRuntimeException(thrown, t1);
            }//EO inner catch block 
        }//EOM 

        protected void rollbackEntity(final V entity, final Throwable t) { /*NOOP*/}//EOM
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
        private MultiRuntimeException accumulatedErrors ; 
        private Hashtable env;

        public ForkContext(BlockingDeque<V> sink, Forker.WorkerFactory<V, T> workerFactory, Hashtable env) {
            this.sink = sink;
            this.workerFactory = workerFactory;
            this.env = env;
            this.accumulatedErrors = new MultiRuntimeException() ; 
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
        
        public final MultiRuntimeException getAccumulatedErrorsSink() { 
            return this.accumulatedErrors ; 
        }//EOM 
    }//EO inner class ForkContext
    
}//EOC
