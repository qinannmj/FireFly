package cn.com.sparkle.firefly.admin.processors;

import java.io.PrintWriter;
import java.io.StringWriter;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.addprocess.AddRequestDealer;
import cn.com.sparkle.firefly.admin.AbstractAdminProcessor;
import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;
import cn.com.sparkle.firefly.stablestorage.io.BufferedFileOut;
import cn.com.sparkle.firefly.state.SelfState;

public class StateProcessor extends AbstractAdminProcessor {
	private Context context;

	public StateProcessor(Context context) {
		this.context = context;
	}

	@Override
	public int commandLength() {
		return 2;
	}

	@Override
	public String processComm(String[] command, AdminLookupHandler handler, PaxosSession session, AddRequest request) {
		SelfState stat = context.getcState().getSelfState();
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		pw.println(String.format("id:%s", command[1]));
		pw.println(String.format("user-port:%s", context.getConfiguration().getClientPort()));
		pw.println(String.format("execute-from-start-count:%s", stat.getExecuteFromStartCount()));
		pw.println(String.format("paxos-pipeline-process-count:%s", stat.getPaxosPipelineProcessCount()));
		pw.println(String.format("recover-record-from-master-loss-count:%s", stat.getRecoverRecordFromMasterLossCount()));
		pw.println(String.format("response-admin-add-request-count:%s", stat.getResponseAdminAddRequestCount()));
		pw.println(String.format("response-custom-add-aequest-count:%s", stat.getResponseCustomAddRequestCount()));
		pw.println(String.format("transport-to-master-count:%s", stat.getTransportToMasterCount()));
		pw.println(String.format("io-wait-queue-size:%s", BufferedFileOut.getWaitQueue().size()));
		pw.println(String.format("instance-executor-queue-size:%s(max:%s)", context.getInstanceExecutor().getSizeInQueue(), context.getInstanceExecutor()
				.maxQueueSize()));
		pw.println(String.format("cur-tcp-package-byte-size:%s(min:%s max:%s)", context.getAddRequestDealer().getCurTcpPackageByteSize(),
				AddRequestDealer.MIN_TCP_PACKAGE_SIZE, context.getAddRequestDealer().MAX_TCP_PACKAGE_SIZE));

		return sw.toString();
	}
}
