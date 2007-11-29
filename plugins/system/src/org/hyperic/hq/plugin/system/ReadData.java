/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2007], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.plugin.system;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.hyperic.hq.product.PluginException;

public class ReadData {
    public static long MAX_BYTES = 1024 * 1024; 
    
    private String _path;
    private long   _fileSize;
    private long   _offset;
    private long   _numBytes;
    private long   _lastModified;
    private String _data;
    
    public ReadData() {}
    
    void populate(String path, long offset, int numBytes) 
        throws PluginException
    {
        if (numBytes > MAX_BYTES)
            throw new PluginException("Requested to read " + numBytes + 
                                      " which exceeds the maximum of " +
                                      MAX_BYTES);
        File f = new File(path);

        _path         = f.getAbsolutePath();
        _fileSize     = f.length();
        _lastModified = f.lastModified();
        
        FileReader fIn = null;
        try {
            fIn = new FileReader(f);

            // Allow negative offsets to read from the end of file
            if (offset < 0)
                offset = _fileSize + offset;

            char[] buf = new char[numBytes];
            fIn.skip(offset);
            int numRead = fIn.read(buf);
            _data     = new String(buf, 0, numRead);
            _offset   = offset;
            _numBytes = numRead;
        } catch(FileNotFoundException e) {
            throw new PluginException("Cannot read [" + path + 
                                      "].  File not found");
        } catch(IOException e) {
            throw new PluginException("IOException reading file: " + 
                                      e.getMessage());
        } finally {
            if (fIn != null) 
                try {fIn.close();} catch(Exception e){}
        }
    }
    
    public String getPath() {
        return _path;
    }
    
    public long getFileSize() {
        return _fileSize;
    }
    
    public long getOffset() {
        return _offset;
    }
    
    public long getNumBytes() {
        return _numBytes;
    }
    
    public long getLastModified() {
        return _lastModified;
    }
    
    public String getData() {
        return _data;
    }
    
    public static ReadData gather(String file, long offset, int numBytes) 
        throws PluginException
    {
        ReadData res = new ReadData();
        res.populate(file, offset, numBytes);
        return res;
    }
}
