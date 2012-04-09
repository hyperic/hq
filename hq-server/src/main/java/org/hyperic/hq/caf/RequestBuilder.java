package org.hyperic.hq.caf;

import java.util.HashSet;
import java.util.Set;

import com.vmware.commonagent.contracts.ValidateParameter;
import com.vmware.commonagent.contracts.builder.BldV1;
import com.vmware.commonagent.contracts.doc.FullyQualifiedClass;
import com.vmware.commonagent.contracts.doc.Parameter;
import com.vmware.commonagent.contracts.doc.PmeBatch;
import com.vmware.commonagent.contracts.doc.PmeCollectInstances;
import com.vmware.commonagent.contracts.doc.PmeInvokeOperation;
import com.vmware.commonagent.contracts.doc.RequestHeader;

public class RequestBuilder {
	public static RequestHeader createRequestHeader() {
		return BldV1.requestHeader()
			.noSamlToken()
			.cafCmdlResponseFormat()
			.noRequestProcessorAddIns()
			.noResponseProcessorAddIns()
			.noEchoPropertyBag()
			.createRequestHeader();
	}
	
	public static PmeBatch createCollectSchema() {
		return BldV1.batch()
			.collectSchemaJob(BldV1.collectSchemaJob()
				.randomJobId()
			.createCollectSchemaJob())
			.noCollectInstancesJobs()
			.noInvokeOperationJobs()
		.createBatch();
	}
	
	public static PmeBatch createCollectInstances(
		final Set<FullyQualifiedClass> fqcCollection,
		final Set<Parameter> operationParameters) {
		ValidateParameter.NotNullOrEmpty("fqcCollection", fqcCollection);
		
		final Set<PmeCollectInstances> collectInstancesCollection =
			new HashSet<PmeCollectInstances>();
		for (final FullyQualifiedClass fqc : fqcCollection) {
			final PmeCollectInstances collectInstancesJob =
				createCollectInstances(fqc, operationParameters);
			collectInstancesCollection.add(collectInstancesJob);
		}
		
		return BldV1.batch()
			.noCollectSchemaJob()
			.collectInstancesJobs(collectInstancesCollection)
			.noInvokeOperationJobs()
		.createBatch();
	}
	
	public static PmeCollectInstances createCollectInstances(
		final FullyQualifiedClass fullyQualifiedClass,
		final Set<Parameter> operationParameters) {
		ValidateParameter.NotNull("fullyQualifiedClass", fullyQualifiedClass);
		
		return BldV1.collectInstancesJob()
			.randomJobId()
			.classSpecifier(BldV1.classSpecifier()
				.fullyQualifiedClass(fullyQualifiedClass)
			.createClassSpecifier())
			.parameters(operationParameters)
			.noAttachments()
			.noInlineAttachments()
		.createCollectInstancesJob();
	}

	public static PmeBatch createInvokeOperation(
		final Set<FullyQualifiedClass> fqcCollection,
		final String operationName,
		final Set<Parameter> operationParameters) {
		ValidateParameter.NotNullOrEmpty("fqcCollection", fqcCollection);
		ValidateParameter.NotNullOrEmpty("operationName", operationName);
		
		final Set<PmeInvokeOperation> invokeOperationCollection =
			new HashSet<PmeInvokeOperation>();
		for (final FullyQualifiedClass fqc : fqcCollection) {
			final PmeInvokeOperation invokeOperationJob = 
				createInvokeOperation(fqc, operationName, operationParameters);
			invokeOperationCollection.add(invokeOperationJob);
		}
		
		return BldV1.batch()
			.noCollectSchemaJob()
			.noCollectInstancesJobs()
			.invokeOperationJobs(invokeOperationCollection)
		.createBatch();
	}
	
	public static PmeInvokeOperation createInvokeOperation(
		final FullyQualifiedClass fullyQualifiedClass,
		final String operationName,
		final Set<Parameter> operationParameters) {
		ValidateParameter.NotNull("fullyQualifiedClass", fullyQualifiedClass);
		ValidateParameter.NotNullOrEmpty("operationName", operationName);
		
		return BldV1.invokeOperationJob()
			.randomJobId()
			.classSpecifier(BldV1.classSpecifier()
				.fullyQualifiedClass(fullyQualifiedClass)
			.createClassSpecifier())
			.operation(BldV1.operation()
				.operationName(operationName)
				.parameters(operationParameters)
			.createOperation())
			.noAttachments()
			.noInlineAttachments()
		.createInvokeOperationJob();
	}
}
