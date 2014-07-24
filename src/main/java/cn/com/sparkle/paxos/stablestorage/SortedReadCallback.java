package cn.com.sparkle.paxos.stablestorage;

import java.util.PriorityQueue;

import com.google.protobuf.GeneratedMessage.Builder;
@SuppressWarnings("rawtypes")
public class SortedReadCallback<T extends Builder<? extends Builder>> implements ReadRecordCallback<T> {
	private ReadRecordCallback<T> callback;
	private long exceptedNextId;
	private PriorityQueue<SortBean> priorityQueue = new PriorityQueue<SortBean>();

	public SortedReadCallback(ReadRecordCallback<T> callback, long exceptedNextId) {
		super();
		this.callback = callback;
		this.exceptedNextId = exceptedNextId;
	}

	@Override
	public void read(long instanceId, T b) {
		priorityQueue.add(new SortBean(instanceId, b));
		while (priorityQueue.peek() != null && exceptedNextId >= priorityQueue.peek().instanceId) {
			SortBean sortBean = priorityQueue.poll();
			if(exceptedNextId == sortBean.instanceId){
				callback.read(sortBean.instanceId, sortBean.b);
				++exceptedNextId;
			}
		}

	}

	public PriorityQueue<SortBean> getPriorityQueue() {
		return priorityQueue;
	}

	private class SortBean implements Comparable<SortBean> {
		private long instanceId;
		private T b;

		public SortBean(long instanceId, T b) {
			super();
			this.instanceId = instanceId;
			this.b = b;
		}

		@Override
		public int compareTo(SortBean o) {
			if (getInstanceId() == o.getInstanceId())
				return 0;
			return getInstanceId() > o.getInstanceId() ? 1 : -1;
		}

		public long getInstanceId() {
			return instanceId;
		}
	}

}
