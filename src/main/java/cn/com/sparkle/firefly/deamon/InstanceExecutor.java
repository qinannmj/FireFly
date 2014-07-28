package cn.com.sparkle.firefly.deamon;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import com.google.protobuf.ByteString;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.ConfigurationException;
import cn.com.sparkle.firefly.event.events.InstanceExecuteEvent;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.model.AdminCommand;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Value;

public class InstanceExecutor extends Thread {
	private final static int MAX_EXECUTE_QUEUE_SIZE = 500;
	private final static Logger logger = Logger.getLogger(InstanceExecutor.class);
	private LinkedBlockingQueue<SuccessfulRecordWrap> fifo = new LinkedBlockingQueue<SuccessfulRecordWrap>(MAX_EXECUTE_QUEUE_SIZE);
	private HandlerInterface userHandlerInterface;
	private volatile long executedInstanceId;
	private Context context;

	public InstanceExecutor(Context context, HandlerInterface userHandlerInterface,long lastExceptInstanceId) {
		super();
		this.userHandlerInterface = userHandlerInterface;
		this.context = context;
		this.setName("Paxos-Instance-Executor");
		this.executedInstanceId = lastExceptInstanceId - 1;
	}

	public void start() {
		super.start();
	}
	
	public long getExecutedInstanceId(){
		return executedInstanceId;
	}
	
	public void execute(SuccessfulRecordWrap recordWrap) {
		SuccessfulRecord record = recordWrap.getRecord();
		if (record.getV().getValues(0).size() == 0) {
			// this is a null command so that it will be not executed
		} else {
			try {
				fifo.put(recordWrap);
			} catch (InterruptedException e) {
			}
		}
	}

	public void run() {
		SuccessfulRecordWrap recordWrap;
		while (true) {
			try {
				recordWrap = fifo.take();
				LinkedList<AddRequestPackage> addRequestPackages = recordWrap.getAddRequestPackages();
				SuccessfulRecord record = recordWrap.getRecord();
				if (record.getV().getType() == ValueType.ADMIN.getValue()) {
					Value v = record.getV();
					AdminCommand c = new AdminCommand(v.getValues(0).toByteArray());
					try {
						// response success
						if(addRequestPackages != null){
							for (AddRequestPackage arp : addRequestPackages) {
								try {
									arp.responseAdminResponse(arp.getValueList().get(0).getMessageId(), true, "");
								} catch (UnsupportedChecksumAlgorithm e) {
									logger.error("unexcepted error", e);
								}
							}
						}
						
						if(c.getType().equals(AdminCommand.ADD_SENATOR)){
							context.getConfiguration().addSenator(c.getAddress(), c.getRoom(), recordWrap.getInstanceId());
						}else if(c.getType().equals(AdminCommand.REMOVE_SENATOR)){
							context.getConfiguration().removeSenator(c.getAddress(), recordWrap.getInstanceId());
						}else{
							throw new ConfigurationException("This command is invalid!");
						}
						
					} catch (ConfigurationException e) {
						logger.error("error admin command", e);
						// response fail
						for (AddRequestPackage arp : addRequestPackages) {
							try {
								arp.responseAdminResponse(arp.getValueList().get(0).getMessageId(), false, e.getMessage());
							} catch (UnsupportedChecksumAlgorithm e1) {
								logger.error("unexcepted error", e1);
							}
						}
					}
					InstanceExecuteEvent.doEventExecutedEvent(context.getEventsManager(), record);
				} else {
					if (recordWrap.getRecord().hasV() || recordWrap.getRecord().getV().getType() != ValueType.ADMIN.getValue()) {// admin
						// command has be executed
						LinkedList<byte[]> customResult = new LinkedList<byte[]>();
						for (int i = 0; i < recordWrap.getRecord().getV().getValuesCount(); ++i) {
							ByteString bs = recordWrap.getRecord().getV().getValues(i);
							customResult.add(bs.toByteArray());
						}
						userHandlerInterface.onLoged(recordWrap, recordWrap.getInstanceId(), customResult, context.getAccountBook());
						InstanceExecuteEvent.doEventExecutedEvent(context.getEventsManager(), recordWrap.getRecord());
						context.getcState().getSelfState().addExecuteFromStartCount(recordWrap.getRecord().getV().getValuesCount());
					}
				}
				executedInstanceId = recordWrap.getInstanceId();
			} catch (InterruptedException e) {
				logger.error("fatal error", e);
			} catch (IOException e) {
				logger.error("fatal error", e);
			}
		}
	}
	public int getSizeInQueue(){
		return fifo.size();
	}
	public int maxQueueSize(){
		return MAX_EXECUTE_QUEUE_SIZE;
	}
}
