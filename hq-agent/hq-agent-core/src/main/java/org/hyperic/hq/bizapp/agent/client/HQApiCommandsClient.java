/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.bizapp.agent.client;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.hqapi1.HQApi;
import org.hyperic.hq.hqapi1.ResourceApi;
import org.hyperic.hq.hqapi1.types.Resource;
import org.hyperic.hq.hqapi1.types.ResourcePrototype;
import org.hyperic.hq.hqapi1.types.ResourcePrototypeResponse;
import org.hyperic.hq.hqapi1.types.ResourcesResponse;
import org.hyperic.hq.hqapi1.types.Response;
import org.hyperic.hq.hqapi1.types.ResponseStatus;
import org.hyperic.hq.hqapi1.types.StatusResponse;

public class HQApiCommandsClient {

	private static final Log log = LogFactory.getLog(HQApiCommandsClient.class.getName());

	private HQApi hqApi;

	public HQApiCommandsClient(HQApi hqApi) {
		this.hqApi = hqApi;
	}

	public HQApi getApi() {
		return this.hqApi;
	}

	public ResourcePrototype getResourcePrototype(String name)
		throws IOException {

		ResourceApi api = hqApi.getResourceApi();
		ResourcePrototypeResponse rpr = api.getResourcePrototype(name);
		assertSuccess(rpr, "getResourcePrototype(" + name + ")", true);
		ResourcePrototype type = rpr.getResourcePrototype();

		if (log.isDebugEnabled()) {
			log.debug("'" + name + "' id=" + type.getId());
		}

		return type;
	}

	public List<Resource> getResources(ResourcePrototype proto, boolean verbose, boolean children) 
		throws IOException {

		ResourceApi api = hqApi.getResourceApi();
		ResourcesResponse rezResponse = api.getResources(proto, verbose,
				children);
		assertSuccess(rezResponse, "Getting all " + proto.getName(), false);

		return rezResponse.getResource();
	}

	public void deleteResource(Resource r) throws IOException {

		if (log.isDebugEnabled()) {
			log.debug("Resource (" + r.getName() + ") no longer exists. "
					+ " Removing from Hyperic inventory.");
		}

		// throttle requests to the hq server to minimize StaleStateExceptions
		// TODO: there needs to be a better way to do this
		try {
			Thread.sleep(2500);
		} catch (InterruptedException e) {
			// Ignore
		}

		ResourceApi rApi = hqApi.getResourceApi();

		// TODO: As a final step, need to check resource availability
		// (must be DOWN) before deleting.

		StatusResponse deleteResponse = rApi.deleteResource(r.getId());
		assertSuccess(deleteResponse, "Delete resource id=" + r.getId(), false);
	}

	public void assertSuccess(Response response, String msg, boolean abort) {
		if (ResponseStatus.SUCCESS.equals(response.getStatus())) {
			return;
		}
		String reason;
		if (response.getError() == null) {
			reason = "unknown";
		} else {
			reason = response.getError().getReasonText();
		}
		msg += ": " + reason;
		if (abort) {
			throw new IllegalStateException(msg);
		} else {
			log.error(msg);
		}
	}
}
