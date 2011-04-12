package org.hyperic.hq.rabbit;

import org.hyperic.hq.agent.*;
import org.hyperic.hq.bizapp.client.AgentCallbackClientException;
import org.hyperic.hq.bizapp.client.BizappCallbackClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

/**
 * @author Helena Edelson
 */
public class BenchmarkClient {

    private BizappCallbackClient bcc;

    /** an AgentCommandsClient */
   // private AmqpCommandOperationService acc;

    public BenchmarkClient(String host, int port) throws AgentConfigException {
       /* this.acc = new AmqpCommandOperationService(new LegacyAgentCommandsClientImpl(new SecureAgentConnection(host, port, "")));
        ProviderInfo providerInfo = new ProviderInfo(AgentCallbackClient.getDefaultProviderURL(host, port, false), "no-auth");
        assertNotNull("'providerInfo' must not be null", providerInfo);
        this.bcc = new BizappCallbackClient(new StaticProviderFetcher(providerInfo), AgentConfig.newInstance());
        assertNotNull("'bizappCallbackClient' must not be null", bcc);*/
    }

    public void rabbitPing(int append) throws IOException, InterruptedException {
        //acc.timedPing(append);
    }

    public void latherPing() throws AgentCallbackClientException {
        bcc.bizappPing();
    }

    public void restart() throws AgentRemoteException, AgentConnectionException {

    }

    public void die() throws AgentRemoteException, AgentConnectionException {

    }

    public String getCurrentAgentBundle() throws AgentRemoteException, AgentConnectionException {
        return null;
    }

    public Map upgrade(String tarFile, String destination) throws AgentRemoteException, AgentConnectionException {
        return null;
    }

    public FileDataResult[] agentSendFileData(FileData[] destFiles, InputStream[] streams) throws AgentRemoteException, AgentConnectionException {
        return new FileDataResult[0];
    }

    public Map<String, Boolean> agentRemoveFile(Collection<String> files) throws AgentRemoteException, AgentConnectionException {
        return null;
    }
}
