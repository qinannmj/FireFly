package cn.com.sparkle.paxos.addprocess;

import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

public class DealState {
	private final static Logger logger = Logger.getLogger(DealState.class);

	public boolean isDealing = false;
	private LinkedList<AddRequestPackage> waitList = new LinkedList<AddRequestPackage>();
	private int queueSize;
	private Condition fullCondition;

	public DealState(ReentrantLock lock, int queueSize) {
		this.fullCondition = lock.newCondition();
		this.queueSize = queueSize;
	}

	public AddRequestPackage pollFirst() {
		if (waitList.size() != 0) {
			fullCondition.signal();
			return waitList.removeFirst();
		} else
			return null;
	}

	public AddRequestPackage peekLast() {
		if (waitList.size() != 0) {
			return waitList.getLast();
		} else
			return null;
	}

	public void add(AddRequestPackage addRequestPackage) {
		if (waitList.size() == queueSize) {
			try {
				fullCondition.await();
			} catch (InterruptedException e) {
				logger.error("fatal error,unexcepted exception : InterruptedException");
			}
		}
		waitList.addLast(addRequestPackage);
	}

	public int size() {
		return waitList.size();
	}
}
