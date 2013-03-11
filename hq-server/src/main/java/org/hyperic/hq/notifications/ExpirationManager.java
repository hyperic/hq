package org.hyperic.hq.notifications;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class ExpirationManager {
    protected final static long EXPIRATION_DURATION = /*10*60*/30*1000;
    @Autowired
    private ThreadPoolTaskScheduler executor;
    protected Queue<Expiration> expirationCandidates = new ConcurrentLinkedQueue<Expiration>();
//    protected AtomicReference<NotificationEndpoint> currExpirationCandidate; 
    protected NotificationEndpoint currExpirationCandidate;
    protected Object notificationEndpointMonitor = new Object();
    protected Thread expirationThread;
    public ExpirationManager() {
//        executor.execute(
        expirationThread = new Thread(
                new Runnable() {
                    public void run() {
                        while (true) {
                            long sleepPeriod;
                            synchronized (notificationEndpointMonitor) {
                                Expiration nextExpiration = ExpirationManager.this.expirationCandidates.poll();
                                if (nextExpiration==null) {
                                    sleepPeriod = ExpirationManager.EXPIRATION_DURATION; 
                                } else {
                                    currExpirationCandidate = nextExpiration.getEndpoint();
                                    sleepPeriod = nextExpiration.getExpirationTime() - System.currentTimeMillis();
                                }
                            }
                            
                            try {
                                Thread.sleep(sleepPeriod);
                            }catch(InterruptedException e) {
                                //TODO~ ?
                            }
                            
                            synchronized (notificationEndpointMonitor) {
                                if (currExpirationCandidate!=null) {
                                    expire(currExpirationCandidate);    
                                }
                            }
                        }
                    }
                });
        expirationThread.start();
    }
    
    
    public void addExpiration(NotificationEndpoint endpoint) {
        Expiration newExpiration = new Expiration(endpoint,System.currentTimeMillis() + ExpirationManager.EXPIRATION_DURATION);
        synchronized (notificationEndpointMonitor) {
            if ((this.currExpirationCandidate==null || !this.currExpirationCandidate.equals(endpoint)) && !this.expirationCandidates.contains(newExpiration)) {
                this.expirationCandidates.add(newExpiration);
            }
        }
    }


    public void removeExpiration(NotificationEndpoint endpoint) {
//        boolean isUnExpiredCurrEndpoint = this.currExpirationCandidate.compareAndSet(endpoint, null);
        synchronized (notificationEndpointMonitor) {
            if (this.currExpirationCandidate!=null && this.currExpirationCandidate.equals(endpoint)) {
                this.currExpirationCandidate=null;
                this.expirationThread.interrupt();
            }
        }
        this.expirationCandidates.remove(new Expiration(endpoint,null));
    }

    protected void expire(NotificationEndpoint expirationCandidate) {
        
    }
    
    protected static class Expiration {
        protected Long expirationTime;
        protected NotificationEndpoint endpoint;
        
        public Expiration(NotificationEndpoint endpoint, Long expirationTime) {
            this.expirationTime = expirationTime;
            this.endpoint = endpoint;
        }
        public Long getExpirationTime() {
            return expirationTime;
        }
        public void setExpirationTime(Long expirationTime) {
            this.expirationTime = expirationTime;
        }
        public NotificationEndpoint getEndpoint() {
            return endpoint;
        }
        public void setEndpoint(NotificationEndpoint endpoint) {
            this.endpoint = endpoint;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((endpoint == null) ? 0 : endpoint.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if(this == obj) return true;
            if(obj == null) return false;
            if(getClass() != obj.getClass()) return false;
            Expiration other = (Expiration) obj;
            if(endpoint == null) {
                if(other.endpoint != null) return false;
            }else if(!endpoint.equals(other.endpoint)) return false;
            return true;
        }
    }
    
    public static void main(String[] args) throws Throwable {
        final Long l0 = new Long(3);
        final AtomicReference<Long> l = new AtomicReference<Long>(l0);
        
        Runnable r0= new Runnable() {
            public void run() {
                synchronized (l0) {
                    int i=0;
                    while(true) {
                        i=1;
//                        System.out.println(".");
                    }
                }
            }
        };
        Runnable r1= new Runnable() {
            public void run() {
                l.set(new Long(4));
                System.out.println(l);
            }
        };
        new Thread(r0).start();
        Thread.sleep(1000*2);
        new Thread(r1).start();
    }//EOM 
}