package cn.com.sparkle.paxos.paxosinstance.paxossender;

public abstract class AbstractPaxosMessageSender implements PaxosMessageSender {
	protected int quorum;

	public AbstractPaxosMessageSender(int quorum) {
		this.quorum = quorum;
	}

	public int getQuorum() {
		return quorum;
	}
}
