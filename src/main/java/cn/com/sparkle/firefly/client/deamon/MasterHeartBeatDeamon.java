package cn.com.sparkle.firefly.client.deamon;

import java.util.HashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.client.Command;
import cn.com.sparkle.firefly.client.CommandAsyncProcessor;
import cn.com.sparkle.firefly.client.PaxosClient.CommandCallBack;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class MasterHeartBeatDeamon extends Thread {
	private final static Logger logger = Logger.getLogger(MasterHeartBeatDeamon.class);
	@SuppressWarnings("unused")
	private boolean debugLog;

	private PriorityBlockingQueue<PriorNode> queue = new PriorityBlockingQueue<PriorNode>(100);

	private HashMap<CommandAsyncProcessor, PriorNode> priorNodeMap = new HashMap<CommandAsyncProcessor, PriorNode>(256);

	private ReentrantLock lock = new ReentrantLock();

	public MasterHeartBeatDeamon(boolean debugLog) {
		this.debugLog = debugLog;
	}

	public void run() {

		while (true) {
			while (queue.peek() != null && queue.peek().nextExecTime < TimeUtil.currentTimeMillis()) {

				PriorNode priorNode = queue.poll();
				if (!priorNode.invalided) {
					Command c = new Command(CommandType.ADMIN_WRITE,-1, null, new BeatHeartCallback(priorNode, queue));
					try {
						priorNode.processor.addCommand(c, 1, TimeUnit.MICROSECONDS);
					} catch (InterruptedException e) {
						logger.error("unexcepted error", e);
					}
				}
			}
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void processorChangeNode(CommandAsyncProcessor processor) {
		try {
			lock.lock();
			PriorNode node = priorNodeMap.get(processor);
			if (node != null) {
				node.invalided = true;
			}
			node = new PriorNode(TimeUtil.currentTimeMillis() + 10, processor);
			priorNodeMap.put(processor, node);
			queue.put(node);
		} finally {
			lock.unlock();
		}
	}

	private static class BeatHeartCallback implements CommandCallBack {
		private long sendTime = TimeUtil.currentTimeMillis();
		private PriorNode priorNode;
		private PriorityBlockingQueue<PriorNode> queue;

		public BeatHeartCallback(PriorNode priorNode, PriorityBlockingQueue<PriorNode> queue) {
			this.priorNode = priorNode;
			this.queue = queue;
		}

		@Override
		public void response(byte[] result,long instanceId) {
			NetNode n = priorNode.processor.getNode();
			int interval = n == null ? 10 : n.getHeartBeatInterval();
			priorNode.nextExecTime = sendTime + interval;
			queue.add(priorNode);
		}
	};

	private static class PriorNode implements Comparable<PriorNode> {
		private long nextExecTime;
		private CommandAsyncProcessor processor;
		private volatile boolean invalided = false;

		public PriorNode(long nextExecTime, CommandAsyncProcessor processor) {
			super();
			this.nextExecTime = nextExecTime;
			this.processor = processor;
		}

		@Override
		public int compareTo(PriorNode o) {
			if (nextExecTime < o.nextExecTime)
				return -1;
			else if (nextExecTime == o.nextExecTime)
				return 0;
			return 1;
		}

	}
}
