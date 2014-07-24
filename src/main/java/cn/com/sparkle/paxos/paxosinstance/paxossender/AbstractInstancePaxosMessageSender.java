package cn.com.sparkle.paxos.paxosinstance.paxossender;

/**
 * only for class safe
 * All instance sender avoid send success message to self,because the success record were write when the paxos vote is successful.
 * @author qinan.qn
 *
 */
public abstract class AbstractInstancePaxosMessageSender extends AbstractPaxosMessageSender {
	public AbstractInstancePaxosMessageSender(int quorum) {
		super(quorum);
	}
}
