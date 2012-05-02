package org.hyperic.hq.caf;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.message.GenericMessage;

import com.vmware.commonagent.common.core.XmlElement;
import com.vmware.commonagent.common.core.XmlUtils;


class CafMessageListener {
	
	private static final Logger _log = LoggerFactory.getLogger(CafMessageListener.class);
	
	/**
	 * Receives messages from CAF, parses them, creates a CafResponse object
	 * and puts it in the CafResultsHolder in case of response message (recative provider
	 * response). 
	 * @param message
	 */
	public void handleMessage(GenericMessage<String> message) {
		XmlElement rootXml;
		CafResponse response = null;
		String messageType;
		try {
			_log.debug("Handling message : " + message.getPayload());
			messageType = (String) message.getHeaders().get("type");
			
			//reactive provider response
			if (messageType.compareTo("response") == 0) {
				rootXml = XmlUtils.parseString(message.getPayload(), "caf:response",
						true);
				response = createResponse(rootXml);
				_log.info("Created CAF response - \n" + response);
				CafResultsHolder.addResult(response.getRequestIdStr(), response);
			}
			//proactive provider event - not yet implemented
			if (messageType.compareTo("event") == 0) {
			}
		} catch (Exception e) {
			_log.error(e.toString());
		}
	}
	
	/**
	 * This will be called in cases where calls to a certain CAF has failed,
	 * like in cases of calling a provider that does no exists
	 */
	public void handleMessage(Message<MessagingException> message) {
		try {
			_log.error(
					"Messaging Exception: " +
					message.getPayload().getMessage() +
					" : "
					+ message.getPayload().getCause().getMessage());
		} catch (Exception e) {
			_log.error(e.getMessage());
		}
	}
	
	/**
	 * Parses the response and returns a CafResponse object
	 * @param rootXml
	 * @throws IOException
	 */
	private CafResponse createResponse(final XmlElement rootXml) throws IOException {			
			CafResponse response = new CafResponse();
			response.setClientIdStr(rootXml.findRequiredAttribute("clientId"));
			response.setRequestIdStr(rootXml.findRequiredAttribute("requestId"));
			response.setPmeIdStr(rootXml.findRequiredAttribute("pmeId"));
			processAttachments(response, rootXml);		
			return response;
		}
	

	/**
	 * Adds all the response attachment to the CafResponse object
	 * @param response
	 * @param responseRoot
	 * @throws IOException
	 */
	private void processAttachments(
		final CafResponse response,
		final XmlElement responseRoot) throws IOException {
		
		Set<XmlElement> elements = new HashSet<XmlElement>();
		final XmlElement topAttachmentCollection =
			responseRoot.findOptionalChild("attachmentCollection");
		if (topAttachmentCollection != null) {
			final List<XmlElement> attChildren = topAttachmentCollection.getAllChildren();
			if (attChildren != null) {
				for (final XmlElement element : attChildren) {
					elements.add(element);
				}
			}
		}

		for (final XmlElement attachment : elements) {
			final String uri = attachment.findRequiredAttribute("uri");
			if (uri.startsWith("inline:")) {
				continue;
			}	
			String encodedUri = uri.substring(7);
			String type = attachment.findRequiredAttribute("type");
			FileInputStream fstream = new FileInputStream(encodedUri);
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			
			while ((strLine = br.readLine()) != null)   {
				if (type.equalsIgnoreCase("stdout")) {
					response.addToStdOut(strLine);
				}
				else if (type.equalsIgnoreCase("stderr")) {
					response.addToStdErr(strLine);
				}
			}

		
		}
	
	}
}
