package org.hyperic.hq.operation.rabbit.demo.ping;

import java.io.IOException;

/**
 * @author Helena Edelson
 */
public interface Ping {

    long ping(int attempts) throws IOException, InterruptedException;
 
}
