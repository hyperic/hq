package org.hyperic.hq.caf;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.message.GenericMessage;

import com.vmware.commonagent.common.core.SinglePmeRequestBuilder;
import com.vmware.commonagent.contracts.ValidateParameter;
import com.vmware.commonagent.contracts.doc.FullyQualifiedClass;
import com.vmware.commonagent.contracts.doc.Parameter;
import com.vmware.commonagent.contracts.doc.PmeBatch;
import com.vmware.commonagent.contracts.doc.PmeContext;
import com.vmware.commonagent.contracts.doc.RequestHeader;
import com.vmware.commonagent.contracts.doc.SinglePmeRequest;
import com.vmware.commonagent.contracts.exceptions.CafException;

/**
 * Central component for executing CAF operations
 */
class CafRequestsExeutor {
	
	private  final UUID clientId;
	private  final String routingKeyPrefix;
	private  final String amqpReplyTo;
	private  final MessageChannel pmeChannel;
    private  final Log log = LogFactory.getLog(CafRequestsExeutor.class);

    /**
     * @param clientId
     * @param routingKeyPrefix
     * @param pmeChannel
     */
    public CafRequestsExeutor(String clientId, String routingKeyPrefix, 
    		MessageChannel pmeChannel) {
    	this.clientId = UUID.fromString(clientId);
    	this.routingKeyPrefix = routingKeyPrefix;
    	this.amqpReplyTo = String.format("%s.%s", routingKeyPrefix, clientId);
    	this.pmeChannel = pmeChannel;
    }
	
	/**
	 * @param pmeId
	 * @throws CafException
	 */
	public void executeCollectSchema(String pmeId) throws CafException{
		log.info("Executing collect schema operation, PME id is '" + pmeId + "'");
		UUID requestId = UUID.randomUUID();
		final RequestHeader requestHeader = RequestBuilder.createRequestHeader();
		final PmeBatch batch = RequestBuilder.createCollectSchema();
		final PmeContext pmeContext = new PmeContext(clientId, requestId, UUID.fromString(pmeId));
		execute(pmeContext, requestHeader, batch, pmeChannel);
	}
	

	/**
	 * @param pmeId
	 * @param fqc
	 * @param operation
	 * @param params
	 * @throws CafException
	 */
	public void executeInvokeOperation(UUID requestId, String pmeId, String fqc, String operation, 
			Map<String, Object> params) throws CafException{
		log.info("Executing CAF invoke operation, PME id is '" + pmeId + "', FQC is '" +
			fqc + "' , method name is '" + operation + "'" );
		if (log.isDebugEnabled()) {
    		String parameters = "";
    		for (String key : params.keySet()) {
    			parameters += key + " = '" + params.get(key) + "' , ";
    		}
    		log.debug("The parameters for the invoke operation are - " 
    				+ parameters);
    	}
		final Set<Parameter> operationParameterCollection = new HashSet<Parameter>();
		final Set<FullyQualifiedClass> fqcCollection = new HashSet<FullyQualifiedClass>();
		for (String key : params.keySet()) {
			operationParameterCollection.add(createParam(key,params.get(key)));
		}
		fqcCollection.add(getFQC(fqc));
		final PmeBatch batch = RequestBuilder.createInvokeOperation(fqcCollection,
				operation, operationParameterCollection);
		final RequestHeader requestHeader = RequestBuilder.createRequestHeader();
		final PmeContext pmeContext = new PmeContext(clientId, requestId, UUID.fromString(pmeId));
		execute(pmeContext, requestHeader, batch, pmeChannel);
	}

	
	/**
	 * @param pmeContext
	 * @param requestHeader
	 * @param batch
	 * @param pmeChannel
	 */
	@SuppressWarnings("serial")
	private void execute(final PmeContext pmeContext, final RequestHeader requestHeader,
			final PmeBatch batch, final MessageChannel pmeChannel) {
			ValidateParameter.NotNull("pmeContext", pmeContext);
			ValidateParameter.NotNull("requestHeader", requestHeader);
			ValidateParameter.NotNull("batch", batch);
			ValidateParameter.NotNull("pmeChannel", pmeChannel);

			final SinglePmeRequest singlePmeReq = new SinglePmeRequest(pmeContext,
				requestHeader, batch);
			final String requestMem = SinglePmeRequestBuilder.build(singlePmeReq);
			pmeChannel.send(new GenericMessage<String>(requestMem, new MessageHeaders(
				new TreeMap<String, Object>() {
					{
						put("send_to", String.format("%s.%s", routingKeyPrefix,
								pmeContext.getPmeIdStr()));
						put("amqp_replyTo", amqpReplyTo);
					}
				})));
		}
	
	/**
	 * @param fqc
	 * @return
	 */
	private FullyQualifiedClass getFQC(String fqc) {
		FullyQualifiedClass fqcObject;
		try {
		fqcObject =  new FullyQualifiedClass(fqc.split(":")[0], fqc.split(":")[1], 
				fqc.split(":")[2]);
		}
		catch (Exception e) {
			throw new CafException(fqc + " is not a valid FQC, should be in the " +
					"format of namespace:className:version");
		}
		return fqcObject;
	}
	
	/**
	 * @param key
	 * @param value
	 * @return
	 */
	private Parameter createParam(String key, Object value) {
		if (value instanceof String) {
			return Parameter.newInstance(key, (String)value);
		}
		if (value instanceof Integer) {
			return Parameter.newInstance(key, (Integer)value);
		}
		if (value instanceof Boolean) {
			return Parameter.newInstance(key, (Boolean)value);
		}
		throw new CafException("Value type for " + key + " is not supported " +
				"CAF parementer type, should be String, Integer or Boolean");
	}
		
}
