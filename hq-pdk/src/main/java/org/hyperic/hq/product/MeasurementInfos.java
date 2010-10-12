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

package org.hyperic.hq.product;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;

/**
 * A collection of {@link MeasurementInfo}s
 * 
 * @author jhickey
 * 
 */
public class MeasurementInfos {

	/**
	 * Constructs a {@link MeasurementInfos} from a byte array
	 * 
	 * @param data
	 *            The byte array
	 * @return The created {@link MeasurementInfos}
	 * @throws EncodingException
	 * @throws InvalidOptionException
	 * @throws InvalidOptionValueException
	 */
	public static MeasurementInfos decode(byte[] data)
			throws EncodingException, InvalidOptionException,
			InvalidOptionValueException {
		try {
			ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
			ObjectInputStream objectStream = new ObjectInputStream(byteStream);
			MeasurementInfos measurements = new MeasurementInfos();

			// Read attributes
			while (true) {
				MeasurementInfo measurementInfo;
				if ((measurementInfo = (MeasurementInfo) objectStream
						.readObject()) == null) {
					break;
				}
				measurements.addMeasurementInfo(measurementInfo);
			}
			return measurements;
		} catch (IOException exc) {
			throw new EncodingException(exc.toString());
		} catch (ClassNotFoundException exc) {
			throw new EncodingException(exc.toString());
		}
	}

	private Set measurements = new HashSet();

	/**
	 * Adds a measurement to the collection
	 * 
	 * @param measurement
	 *            The measurement to add
	 */
	public void addMeasurementInfo(MeasurementInfo measurement) {
		measurements.add(measurement);
	}

	/**
	 * Constructs a byte array from this collection of {@link MeasurementInfo}s
	 * 
	 * @return The encoded bytes representing this object
	 * @throws EncodingException
	 */
	public byte[] encode() throws EncodingException {
		ObjectOutputStream objectStream = null;
		byte[] retVal = null;
		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			objectStream = new ObjectOutputStream(byteStream);

			for (Iterator iterator = measurements.iterator(); iterator
					.hasNext();) {
				objectStream.writeObject((MeasurementInfo) iterator.next());
			}
			objectStream.writeObject(null);
			objectStream.flush();
			retVal = byteStream.toByteArray();
		} catch (IOException exc) {
			throw new EncodingException(exc.toString());
		} finally {
			// ObjectStreams MUST be closed.
			if (objectStream != null)
				try {
					objectStream.close();
				} catch (Exception ex) {
				}
		}
		return retVal;
	}

	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj.getClass().equals(this.getClass()))) {
			return false;
		}
		// Can't use equals on Measurements set b/c MeasurementInfo does not
		// implement equals. I'm too nervous about impact of that to change
		// existing object
		return getMeasurementNames().equals(((MeasurementInfos) obj).getMeasurementNames());
	}

	/**
	 * 
	 * @return A Set of String representing the names of the Measurements contained by this collection
	 */
	public Set getMeasurementNames() {
		Set measurementNames = new HashSet();
		for(Iterator iterator = getMeasurements().iterator();iterator.hasNext();) {
			measurementNames.add(((MeasurementInfo)iterator.next()).getName());
		}
		return measurementNames;
	}
	
	public MeasurementInfo getMeasurement(String name) {
		for(Iterator iterator = getMeasurements().iterator();iterator.hasNext();) {
			MeasurementInfo measurement = (MeasurementInfo)iterator.next();
			if(measurement.getName().equals(name)) {
				return measurement;
			}
		}
		return null;
	}

	/**
	 * 
	 * @return The {@link MeasurementInfo}s in this collection
	 */
	public Set getMeasurements() {
		return measurements;
	}

	public int hashCode() {
		return getMeasurementNames().hashCode();
	}
	
	public String toString() {
		final StringBuilder measurementString = new StringBuilder("MeasurementInfos[measurementNames=").append(getMeasurementNames()).append("]");
		return measurementString.toString();
	}

}
