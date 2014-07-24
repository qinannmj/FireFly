package cn.com.sparkle.paxos.client;

import cn.com.sparkle.paxos.client.PaxosClient.CommandCallBack;
import cn.com.sparkle.paxos.model.AddRequest.CommandType;

public class Command {
	private CommandType commandType;
	private byte[] value;
	private CommandCallBack callBack;

	public Command(CommandType commandType, byte[] value, CommandCallBack callBack) {
		super();
		this.commandType = commandType;
		this.value = value;
		this.callBack = callBack;
	}

	public CommandType getCommandType() {
		return commandType;
	}

	public byte[] getValue() {
		return value;
	}

	public boolean hasCallback() {
		return callBack != null;
	}

	public void finish(byte[] response) {
		if (callBack != null) {
			callBack.response(response);
		}
	}
}
