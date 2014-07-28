package cn.com.sparkle.firefly.model;

public class AddRequest {
	public static enum CommandType {
		ADMIN_WRITE((byte) 0), ADMIN_READ((byte) 1), ADMIN_CONSISTENTLY_READ((byte) 2), USER_WRITE((byte) 3), USER_READ((byte) 4), USER_CONSISTENTLY_READ(
				(byte) 5), USER_MASTER_READ((byte) 6), ADMIN_READ_TRANSPORT_MASTER((byte) 7);
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
				return ADMIN_CONSISTENTLY_READ;
			case 3:
				return USER_WRITE;
			case 4:
				return USER_READ;
			case 5:
				return USER_CONSISTENTLY_READ;
			case 6:
				return USER_MASTER_READ;
			case 7:
				return ADMIN_READ_TRANSPORT_MASTER;
			default:
				throw new RuntimeException("unsupported type:" + value);
			}
		}

		public boolean forceTransportToMaster() {
			return this == CommandType.USER_WRITE || this == CommandType.USER_MASTER_READ || this == CommandType.ADMIN_WRITE
					|| this == CommandType.ADMIN_READ_TRANSPORT_MASTER;
		}

		public boolean isWrite() {
			return this == CommandType.USER_WRITE || this == CommandType.ADMIN_WRITE;
		}

		public boolean isAdmin() {
			return this == CommandType.ADMIN_READ || this == CommandType.ADMIN_READ_TRANSPORT_MASTER || this == CommandType.ADMIN_WRITE;
		}
		public boolean isConsistentlyRead(){
			return this == CommandType.ADMIN_CONSISTENTLY_READ || this == CommandType.USER_CONSISTENTLY_READ; 
		}
	}

	private long messageId;
	private CommandType commandType;
	private byte[] value;
	private long instanceId;

	public AddRequest(long messageId, CommandType commandType, byte[] value, long instanceId) {
		super();
		this.messageId = messageId;
		this.value = value;
		this.commandType = commandType;
		this.instanceId = instanceId;
	}

	public long getMessageId() {
		return messageId;
	}

	public long getInstanceId() {
		return instanceId;
	}

	public CommandType getCommandType() {
		return commandType;
	}

	public byte[] getValue() {
		return value;
	}
}
