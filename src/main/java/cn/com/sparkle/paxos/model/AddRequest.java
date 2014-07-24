package cn.com.sparkle.paxos.model;

public class AddRequest {
	public static enum CommandType {
		ADMIN_WRITE((byte) 0), ADMIN_READ((byte) 1), USER_WRITE((byte) 2), USER_READ((byte) 3), USER_MASTER_READ((byte) 4), ADMIN_READ_TRANSPORT_MASTER(
				(byte) 5);
		private final byte value;

		private CommandType(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}

		public static CommandType getType(int value) {
			switch (value) {
			case 0:
				return ADMIN_WRITE;
			case 1:
				return ADMIN_READ;
			case 2:
				return USER_WRITE;
			case 3:
				return USER_READ;
			case 4:
				return USER_MASTER_READ;
			case 5:
				return ADMIN_READ_TRANSPORT_MASTER;
			default:
				throw new RuntimeException("unsupported type:" + value);
			}
		}
	}

	private long messageId;
	private CommandType commandType;
	private byte[] value;

	public AddRequest(long messageId, CommandType commandType, byte[] value) {
		super();
		this.messageId = messageId;
		this.value = value;
		this.commandType = commandType;
	}

	public long getMessageId() {
		return messageId;
	}

	public boolean forceTransportToMaster() {
		return this.commandType == CommandType.USER_WRITE || this.commandType == CommandType.USER_MASTER_READ || this.commandType == CommandType.ADMIN_WRITE
				|| this.commandType == CommandType.ADMIN_READ_TRANSPORT_MASTER;
	}

	public boolean isWrite() {
		return this.commandType == CommandType.USER_WRITE || this.commandType == CommandType.ADMIN_WRITE;
	}

	public boolean isAdmin() {
		return this.commandType == CommandType.ADMIN_READ || this.commandType == CommandType.ADMIN_READ_TRANSPORT_MASTER
				|| this.commandType == CommandType.ADMIN_WRITE;
	}

	public CommandType getCommandType() {
		return commandType;
	}

	public byte[] getValue() {
		return value;
	}
}
