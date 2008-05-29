/*
 * NOTE: This copyright does *not* cover user programs that use HQ program
 * services by normal system calls through the application program interfaces
 * provided as part of the Hyperic Plug-in Development Kit or the Hyperic Client
 * Development Kit - this is merely considered normal use of the program, and
 * does *not* fall under the heading of "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006, 2007, 2008], Hyperic, Inc. This file is part
 * of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify it under the terms
 * version 2 of the GNU General Public License as published by the Free Software
 * Foundation. This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package org.hyperic.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * A <code>RandomByteInputStream</code> supplies random bytes corresponding to
 * the ascii character range that may be read from the stream. It has an
 * associated size and maintains an internal counter to test whether the end of
 * the stream is reached during a <code>read</code> operation.
 * <p>
 * Closing a <tt>RandomByteInputStream</tt> has no effect. The methods in this
 * class can be called after the stream has been closed without generating an
 * <tt>IOException</tt>.
 * 
 */
public class RandomByteInputStream extends InputStream {

    /**
     * The index of the next character to read from the input stream buffer.
     * This value should always be nonnegative and not larger than the value of
     * <code>count</code>.
     */
    protected int pos;

    /**
     * The number of bytes that can be read from this input stream.
     */
    protected int count;

    /**
     * Creates a <code>RandomByteInputStream</code> of the specified
     * <code>length</code>.
     * 
     * @param length the number of bytes available from this input stream.
     */
    public RandomByteInputStream(int length) {
        this.pos = 0;
        this.count = length;
    }

    // initializes the array to a random identical ascii value
    private void getRandomBytes(byte[] b) {
        char t = (char) (Math.random() * ('z' - 'A') + 'A');
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) t;
        }
    }

    /**
     * Reads the next byte of data from this input stream. The value byte is
     * returned as an <code>int</code> in the range <code>0</code> to
     * <code>255</code>. If no byte is available because the end of the
     * stream has been reached, the value <code>-1</code> is returned.
     * <p>
     * This <code>read</code> method cannot block.
     * 
     * @return the next byte of data, or <code>-1</code> if the end of the
     *         stream has been reached.
     */
    public synchronized int read() {
        if (pos < count) {
            byte[] b = new byte[1];
            getRandomBytes(b);
            return b[0] & 0xff;
        }
        else {
            return -1;
        }
    }

    /**
     * Reads up to <code>len</code> bytes of data into an array of bytes from
     * this input stream. If <code>pos</code> equals <code>count</code>,
     * then <code>-1</code> is returned to indicate end of file. Otherwise,
     * the number <code>k</code> of bytes read is equal to the smaller of
     * <code>len</code> and <code>count-pos</code>. If <code>k</code> is
     * positive, then k bytes are copied into <code>b[off]</code> through
     * <code>b[off+k-1]</code> in the manner performed by
     * <code>System.arraycopy</code>. The value <code>k</code> is added
     * into <code>pos</code> and <code>k</code> is returned.
     * <p>
     * This <code>read</code> method cannot block.
     * 
     * @param b the buffer into which the data is read.
     * @param off the start offset of the data.
     * @param len the maximum number of bytes read.
     * @return the total number of bytes read into the buffer, or
     *         <code>-1</code> if there is no more data because the end of the
     *         stream has been reached.
     */
    public synchronized int read(byte[] b, int off, int len) {
        if (b == null) {
            throw new NullPointerException();
        }
        else if ((off < 0) || (off > b.length) || (len < 0)
                || ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        }
        if (pos >= count) {
            return -1;
        }
        if (pos + len > count) {
            len = count - pos;
        }
        if (len <= 0) {
            return 0;
        }
        byte[] buf = new byte[len];
        getRandomBytes(buf);
        System.arraycopy(buf, 0, b, off, len);
        pos += len;
        return len;
    }

    /**
     * Skips <code>n</code> bytes of input from this input stream. Fewer bytes
     * might be skipped if the end of the input stream is reached. The actual
     * number <code>k</code> of bytes to be skipped is equal to the smaller of
     * <code>n</code> and <code>count-pos</code>. The value <code>k</code>
     * is added into <code>pos</code> and <code>k</code> is returned.
     * 
     * @param n the number of bytes to be skipped.
     * @return the actual number of bytes skipped.
     */
    public synchronized long skip(long n) {
        if (pos + n > count) {
            n = count - pos;
        }
        if (n < 0) {
            return 0;
        }
        pos += n;
        return n;
    }

    /**
     * Returns the number of bytes that can be read from this input stream
     * without blocking. The value returned is <code>count&nbsp;- pos</code>,
     * which is the number of bytes remaining to be read from the input buffer.
     * 
     * @return the number of bytes that can be read from the input stream
     *         without blocking.
     */
    public synchronized int available() {
        return count - pos;
    }

    /**
     * Tests if this <code>InputStream</code> supports mark/reset. The
     * <code>markSupported</code> method of <code>RandomByteInputStream</code>
     * always returns <code>false</code>.
     * 
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * The <code>mark</code> method of <code>RandomByteInputStream</code>
     * always throws an <code>UnsupportedOperationException</code>.
     * 
     */
    public void mark(int readAheadLimit) {
        throw new UnsupportedOperationException("Operation not supported!");
    }

    /**
     * The <code>reset</code> method of <code>RandomByteInputStream</code>
     * always throws an <code>UnsupportedOperationException</code>.
     * 
     */
    public synchronized void reset() {
        throw new UnsupportedOperationException("Operation not supported!");
    }

    /**
     * Closing a <tt>RandomByteInputStream</tt> has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an <tt>IOException</tt>.
     * <p>
     */
    public void close() throws IOException {
    }

}
