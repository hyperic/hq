/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.maven.HQplugins;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Administrator
 */
class StreamConnector extends Thread {

    private final InputStream stream;
    private final PrintStream out;

    public StreamConnector(InputStream errorStream, PrintStream out) {
        this.stream = errorStream;
        this.out = out;
    }

    @Override
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        try {
            String line = null;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        } catch (IOException ex) {
            Logger.getLogger(StreamConnector.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                br.close();
            } catch (IOException ex) {
                Logger.getLogger(StreamConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
