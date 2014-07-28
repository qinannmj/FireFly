package cn.com.sparkle.firefly.client;

import cn.com.sparkle.firefly.client.PaxosClient.CommandCallBack;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;

public class Command {
	private CommandType commandType;
	private byte[] value;
	private long instanceId = -1;
	private CommandCallBack callBack;

	public Command(CommandType commandType,long instanceId, byte[] value, CommandCallBack callBack) {
		super();
		this.instanceId = instanceId;
		this.commandType = commandType;
		this.value = value;
		this.callBack = callBack;
	}

	public CommandType getCommandType() {
		return commandType;
	}
	
	public long getInstanceId() {
		return instanceId;
	}

	public byte[] getValue() {
		return value;
	}

	public boolean hasCallback() {
		return callBack != null;
	}

	public void finish(byte[] response,long instanceId) {
		if (callBack != null) {
			callBack.response(response,instanceId);
		}
	}
}
