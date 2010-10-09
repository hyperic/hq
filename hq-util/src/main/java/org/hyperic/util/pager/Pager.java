/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.util.pager;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Implements a generic pager. What is a pager? Let's say you
 * have a large collection of objects. You're interested in breaking the
 * mammoth list out into a number pages, each with 25 items on it.
 * You're interested in returning page #17 of such a collection.
 * Why bother implementing the "skip past a bunch of things, then
 * return pagesize items in the resultant colleciton" over and over
 * again.
 * 
 * You can also have the elements go through a _processor that you
 * supply as they move from the source collection to the
 * destination collection.
 */
// TODO: G
public class Pager {
    public static final String DEFAULT_PROCESSOR_CLASSNAME = DefaultPagerProcessor.class.getName();
    private static final Map PAGER_PROCESSOR_MAP = Collections.synchronizedMap(new HashMap());

    private PagerProcessor _processor;
    private boolean _skipNulls = false;
    private PagerEventHandler _eventHandler;

    public Pager(PagerProcessor processor) {
        _processor = processor;
        _skipNulls = false;
        _eventHandler = null;

        if (_processor instanceof PagerProcessorExt) {
            _skipNulls = ((PagerProcessorExt) _processor).skipNulls();
            _eventHandler = ((PagerProcessorExt) _processor).getEventHandler();
        }
    }

    public static Pager getDefaultPager() {
        try {
            return getPager(DEFAULT_PROCESSOR_CLASSNAME);
        } catch (Exception e) {
            throw new RuntimeException("This should never happen", e);
        }
    }

    /**
     * Get a pager based on the PagerProcessor supplied.
     */
    public static Pager getPager(String className)
        throws InstantiationException, IllegalAccessException,
        ClassNotFoundException {
        Pager p = (Pager) PAGER_PROCESSOR_MAP.get(className);

        if (p == null) {
            PagerProcessor processor = (PagerProcessor)
                                       Class.forName(className).newInstance();
            p = new Pager(processor);
            PAGER_PROCESSOR_MAP.put(className, p);
        }
        return p;
    }

    /**
     * Seek to the specified pagenum in the source collection and
     * return pagsize numberof of elements in the List.
     * If pagenum or pagesize is -1, then everything in the
     * source collection will be returned.
     * 
     * @param source The source collection to seek through.
     * @param pagenum The page number to seek to. If there not
     *        enough pages in the collection, then an empty list will be
     *        returned.
     * @param pagesize The size of each page.
     * @return PageList containing results of seek.
     */
    public PageList seek(Collection source, int pagenum, int pagesize) {
        return seek(source, pagenum, pagesize, null);
    }

    /**
     * Seek to the specified pagenum in the source collection and return pagsize
     * numberof of elements in the List, as specified the PageControl object.
     * If pagenum or pagesize is -1, then everything in the
     * source collection will be returned.
     * 
     * @param source The source collection to seek through.
     * @param pc The PageControl object to use to control paging.
     * @return PageList containing results of seek.
     */
    public PageList seek(Collection source, PageControl pc) {
        if (pc == null)
            pc = PageControl.PAGE_ALL;

        return seek(source, pc.getPagenum(), pc.getPagesize(), null);
    }

    public PageList seek(Collection source, PageControl pc, Object procData) {
        if (pc == null)
            pc = PageControl.PAGE_ALL;
        return seek(source, pc.getPagenum(), pc.getPagesize(), procData);
    }

    /**
     * Seek to the specified pagenum in the source collection and
     * return pagsize numberof of elements in the List.
     * If pagenum or pagesize is -1, then everything in the
     * source collection will be returned.
     * 
     * @param source The source collection to seek through.
     * @param pagenum The page number to seek to. If there not
     *        enough pages in the collection, then an empty list will be
     *        returned.
     * @param pagesize The size of each page.
     * @param procData - any data object required by the _processor.
     * @return PageList containing results of seek.
     */
    public PageList seek(Collection source, int pagenum, int pagesize,
                         Object procData) {
        PageList dest = new PageList();
        dest.setTotalSize(seek(source, dest, pagenum, pagesize, procData));
        return dest;
    }

    /**
     * Seek to the specified pagenum in the source collection and place
     * pagesize number of elements into the dest collection.
     * If pagenum or pagesize is -1, then everything in the
     * source collection will be placed in the dest collection.
     * 
     * @param source The source collection to seek through.
     * @param dest The collection to place results into.
     * @param pagenum The page number to seek to. If there not
     *        enough pages in the collection, then an empty list will be
     *        returned.
     * @param pagesize The size of each page.
     */
    public void seek(Collection source, Collection dest, int pagenum,
                     int pagesize) {
        seek(source, dest, pagenum, pagesize, null);
    }

    /**
     * Seek to the specified pagenum in the source collection and place
     * pagesize number of elements into the dest collection.
     * If pagenum or pagesize is -1, then everything in the
     * source collection will be placed in the dest collection.
     * 
     * @param source The source collection to seek through.
     * @param dest The collection to place results into.
     * @param pagenum The page number to seek to. If there not
     *        enough pages in the collection, then an empty list will be
     *        returned.
     * @param pagesize The size of each page.
     * @param procData any object required to process the item.
     */
    public int seek(Collection source, Collection dest, int pagenum,
                    int pagesize, Object procData) {
        Iterator iter = source.iterator();
        int i, currentPage, size = source.size();

        if (pagesize == -1 || pagenum == -1) {
            pagenum = 0;
            pagesize = Integer.MAX_VALUE;
        }

        for (i = 0, currentPage = 0; iter.hasNext() && currentPage < pagenum; i++, currentPage += (i % pagesize == 0) ? 1
                                                                                                                     : 0) {
            iter.next();
        }

        if (_eventHandler != null)
            _eventHandler.init();

        if (_skipNulls) {
            Object elt;
            while (iter.hasNext()) {
                if (_processor instanceof PagerProcessorExt)
                    elt = ((PagerProcessorExt) _processor)
                                                          .processElement(iter.next(), procData);
                else
                    elt = _processor.processElement(iter.next());
                if (elt == null) {
                    size--;
                    continue;
                }

                // Need to keep accurate count, so gotta keep checking
                if (dest.size() < pagesize) {
                    dest.add(elt);
                } else if (procData == null) {
                    break;
                }
            }
        } else {
            while (iter.hasNext() && dest.size() < pagesize) {
                dest.add(_processor.processElement(iter.next()));
            }
        }

        if (_eventHandler != null)
            _eventHandler.cleanup();

        return size;
    }

    /**
     * Seek to the specified pagenum in the source collection and place
     * pagesize number of elements into the dest collection. Unlike,
     * seek(), all items are passed to the Processor or ProcessorExt
     * regardless whether they are placed in dest collection. If pagenum
     * or pagesize is -1, then everything in the source collection will
     * be placed in the dest collection.
     * 
     * @param source The source collection to seek through.
     * @param pagenum The page number to seek to. If there not
     *        enough pages in the collection, then an empty list will be
     *        returned.
     * @param pagesize The size of each page.
     * @param procData any object required to process the item.
     */
    public PageList seekAll(Collection source, int pagenum, int pagesize,
                            Object procData) {
        PageList dest = new PageList();
        dest.setTotalSize(seekAll(source, dest, pagenum, pagesize, procData));
        return dest;
    }

    /**
     * Seek to the specified pagenum in the source collection and place
     * pagesize number of elements into the dest collection. Unlike,
     * seek(), all items are passed to the Processor or ProcessorExt
     * regardless whether they are placed in dest collection. If pagenum
     * or pagesize is -1, then everything in the source collection will
     * be placed in the dest collection.
     * 
     * @param source The source collection to seek through.
     * @param dest The collection to place results into.
     * @param pagenum The page number to seek to. If there not
     *        enough pages in the collection, then an empty list will be
     *        returned.
     * @param pagesize The size of each page.
     * @param procData any object required to process the item.
     */
    public int seekAll(Collection source, Collection dest, int pagenum,
                       int pagesize, Object procData) {
        Iterator iter = source.iterator();
        int i, currentPage, size = source.size();

        if (pagesize == -1 || pagenum == -1) {
            pagenum = 0;
            pagesize = Integer.MAX_VALUE;
        }

        // PR:8434 : Multi-part paging fixes.
        // 1.) Invoke the pager _processor external which in many cases may
        // be keeping track of element [in|ex]clusion.
        // 2.) The counter 'i' is used with modulus arithmetic to determine
        // which page we should be on. Don't increment it if the proc-ext
        // indicated that the element should not be paged.
        // 3.) 'i' begins it's existance at 0. Zero modulus anything yields
        // zero. So the ternary expression needs to check for this initial
        // condition and not increment the page number.
        for (i = 0, currentPage = 0; iter.hasNext() && currentPage < pagenum; currentPage += (i != 0 && i % pagesize == 0) ? 1
                                                                                                                          : 0) {
            Object ret = null;

            if (_processor instanceof PagerProcessorExt) {
                ret = ((PagerProcessorExt) _processor).processElement(iter.next(), procData);
            } else {
                ret = _processor.processElement(iter.next());
            }

            if (ret != null) {
                i++;
            }
        }

        if (_eventHandler != null)
            _eventHandler.init();

        if (_skipNulls) {
            Object elt;
            while (iter.hasNext()) {
                if (_processor instanceof PagerProcessorExt)
                    elt = ((PagerProcessorExt) _processor)
                                                          .processElement(iter.next(), procData);
                else
                    elt = _processor.processElement(iter.next());

                if (elt == null) {
                    size--;
                    continue;
                }

                if (dest.size() < pagesize)
                    dest.add(elt);
            }
        } else {
            while (iter.hasNext()) {
                Object elt = _processor.processElement(iter.next());
                if (dest.size() < pagesize)
                    dest.add(elt);
            }
        }

        if (_eventHandler != null)
            _eventHandler.cleanup();

        return size;
    }

    /**
     * Process all objects in the source page list and return the destination
     * page list with the same total size
     */
    public PageList processAll(PageList source) {
        PageList dest = new PageList();
        int size = source.getTotalSize();
        for (Iterator it = source.iterator(); it.hasNext();) {
            Object elt = this._processor.processElement(it.next());
            if (elt == null) {
                size--;
                continue;
            }
            dest.add(elt);
        }

        dest.setTotalSize(size);
        return dest;
    }

    public Object processOne(Object one) {
        return _processor.processElement(one);
    }
}
