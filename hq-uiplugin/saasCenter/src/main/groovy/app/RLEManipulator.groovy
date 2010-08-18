/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

class RLEManipulator {
    /**
     * Returns an array of timestamps representing the boundaries of all the
     * RLE chunks passed.
     *
     * @param rleLists a list of lists (of rle data).
     *                ex:  [
     *                       [ [start: 10, end: 20, value: 0.0 ],
     *                         [start: 20, end: 25, value: 1.0 ],
     *                         [start: 25, end: 50, value: 0.0 ] ],
     *
     *                       [ [start: 2,  end: 15, value: 1.0 ],
     *                         [start: 15, end: 22, value: 0.0 ],
     *                         [start: 22, end: 45, value: 1.0 ] ]
     *                     ]
     *
     * Given the example rleData above, the result will be:
     *     [ 2, 10, 15, 20, 22, 25, 45, 50 ]
     */
    List getBoundaries(List rleLists) {
        List res = []
        rleLists.each { rleList ->
            rleList.each { rleEntry ->
                res << rleEntry.start
                res << rleEntry.end
            }
        }
        res.unique().sort()
    }
    
    /**
     * Combine multiple lists of RLEs into 1.  The newValue() closure will be
     * called with a list of values (from size() == 1 to rleLists.size())
     * consisting of the # of values from the rlelists lying within a
     * time slice.
     *
     * @param start    Start of the time range which the result should contain
     * @param end      End of the time range which the result should contain
     * @param rleLists a list of lists (of rle data).  See getBoundaries()
     *
     * @return Given the example in getBoundaries() the return value should yield:
     *             [
     *               [start: 2,  end: 10, value: 1.0 ],
     *               [start: 10, end: 15, value: 0.0 ],
     *               [start: 15, end: 20, value: 0.0 ],
     *               [start: 20, end: 22, value: 0.0 ],
     *               [start: 22, end: 25, value: 1.0 ],
     *               [start: 25, end: 45, value: 0.0 ],
     *               [start: 45, end: 50, value: 0.0 ]
     *             ]
     */
    List combineRLELists(List rleLists, Closure combineValues) {
        List timeStamps = getBoundaries(rleLists)
    
        List res = []
    
         /**
          * For each return RLE, determine all source RLEs which fall within that
          * time period.  Pass them all to the callback to determine the merged
          * RLE value.
          */
        for (i in 0..<timeStamps.size() - 1) {
            def rle = [start: timeStamps[i],
                       end:   timeStamps[i+1],
                       value: null]
    
            def rleValues = []
            rleLists.each { rleList ->
                rleList.each { rleEntry ->
                    if (rleEntry.start <= rle.start &&
                        rleEntry.end >= rle.end)
                    {
                        rleValues << rleEntry.value
                    }
                }
            }
            rle.value = combineValues(rle.start, rle.end, rleValues)
            res << rle
        }
    
        res
    }
    
    /**
     * Squish an RLElist where adjacent values which are the same are combined.
     *
     * @return Given the example in getBoundaries() the return value should yield:
     *             [
     *               [start: 2,  end: 15, value: 1.0 ],
     *               [start: 15, end: 20, value: 0.0 ],
     *               [start: 20, end: 50, value: 1.0 ],
     *             ]
     *         ... provided the requirement is that if at least 2 values for each
     *         given timeslice is 0, the combined value is 0
     */
    List squishList(List rleList) {
        List res = []
        if (rleList.size() <= 1) {
            return rleList
        }
    
        res << rleList[0]
        int resIdx = 0
        for (int i=1; i<rleList.size(); i++) {
            if (res[resIdx].value == rleList[i].value) {
                res[resIdx].end = rleList[i].end
            } else {
                res << [:] + rleList[i]
                resIdx++
            }
        }
        res
    }
    
    /**
     * Constrain an rleList to a time range.  The result will be RLEs
     * which fit exactly within the time range.  The input rleList will be
     * either expanded or contracted to accomodate this (if expanded, the
     * values will be filled with 'fillValue'
     */
    List constrain(List rleList, long start, long end, double fillValue) {
        rleList = [] + rleList // Copy the list
    
        // Expand list even earlier if start is before our window
        if (rleList[0].start > start) {
            rleList = [[start: start,
                        end:   rleList[0].start,
                        value: fillValue]] + rleList
        }
    
        // Expand list later if end is later than our window
        if (rleList[-1].end < end) {
            rleList += [[start: rleList[-1].end,
                         end:   end,
                         value: fillValue]]
        }
    
        // Shrink list from front if start is later than the rle elements
        for (Iterator i=rleList.iterator(); i.hasNext(); ) {
            Map ent = i.next()
    
            if (ent.end <= start) {
                i.remove()
            } else if (ent.start < start) {
                ent.start = start
            }
        }
    
        // Shrink list from da rear
        for (Iterator i=rleList.iterator(); i.hasNext(); ) {
            Map ent = i.next()
    
            if (ent.end > end && ent.start < end) {
                ent.end = end
            } else if (ent.start >= end) {
                i.remove()
            }
        }
        rleList
    }
}