/*
 * Copyright (c) 2015 VMware, Inc.  All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.hyperic.hq.measurement.agent.server;

import org.apache.commons.lang.builder.ToStringBuilder;

public class SendBatchResult {

	public enum StatusBatchResult {
		SUCCESS_ALL, ERROR_ALL, SUCCESS_BATCH, ERROR_BATCH
	}

	private long timeStamp = 0; // in milliseconds
	private StatusBatchResult status; 

	public SendBatchResult(StatusBatchResult status, long timeStamp) {
		this.timeStamp = timeStamp;
		this.status = status;
	}

	public SendBatchResult(StatusBatchResult status) {
		this.status = status;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public StatusBatchResult getStatus() {
		return status;
	}

	public boolean isDone() {
		return StatusBatchResult.ERROR_ALL.equals(status)
				|| StatusBatchResult.SUCCESS_ALL.equals(status);

	}

	@Override
	public String toString() {
		return new ToStringBuilder(this).append("Status", status.toString())
				.append("Timestamp", timeStamp).toString();

	}

}
