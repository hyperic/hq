/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic .
 *
 *  Hyperic  is free software; you can redistribute it and/or modify
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
package org.hyperic.hq.plugin.rabbitmq.volumetrics;

/**
 * MovingAverageCumulativeHistory 
 * @author Helena Edelson
 * @author Dave Syer
 */
public class MovingAverageCumulativeHistory {

    private int count;
	private double weight;
	private double sum;
	private double sumSquares;
	private double min;
	private double max;
	private final double decay;

	public MovingAverageCumulativeHistory(int window) {
		this.decay = 1 - 1. / window;
	}

	public void append(double value) {
		if (value > max || count == 0)
			max = value;
		if (value < min || count == 0)
			min = value;
		sum = decay * sum + value;
		sumSquares = decay * sumSquares + value * value;
		weight = decay * weight + 1;
		count++;
	}

	public int getCount() {
		return count;
	}

	public double getMean() {
		return weight>0 ? sum / weight : 0.;
	}

	public double getStandardDeviation() {
		double mean = getMean();
		return weight>0 ? Math.sqrt(sumSquares / weight - mean * mean) : 0.;
	}

	public double getMax() {
		return max;
	}

	public double getMin() {
		return min;
	}

	@Override
	public String toString() {
		return String.format("[N=%d, min=%f, max=%f, mean=%f, sigma=%f]", count, min, max, getMean(),
				getStandardDeviation());
	}
    
}
