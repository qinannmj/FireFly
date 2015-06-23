package cn.com.sparkle.raptor.core.collections;

import java.lang.reflect.Array;

/**
 * The class is suited to limit the quene size.But the queue can't be resize when the number of elements 
 * in the queue over the maximum size.
 * Note: the queue is only suited to one thread to read ,the other thread to write.
 * 		And when write or read fail ,the code of you should retry after Thread.sleep(n).
 */
public class MaximumSizeArrayCycleQueue<T> implements Queue<T> {
	private T[] queue;
	private int remain;
	private volatile int s;
	private volatile int e;

	public MaximumSizeArrayCycleQueue(Class<T> type) {
		this(type, 10);
	}

	@SuppressWarnings("unchecked")
	public MaximumSizeArrayCycleQueue(Class<T> type, int size) {
		int tsize = 2;
		while (tsize <= size)
			tsize = tsize << 2;
		queue = (T[]) Array.newInstance(type, tsize);
		remain = tsize - 1;
		s = 0;
		e = 1;
	}

	public void push(T t) throws QueueFullException {
		if (e != s) {
			int es = (e + 1) & remain;
			queue[e] = t;
			e = es;
		} else {
			throw new QueueFullException("The queue is full.You should increase the size of the queue.");
		}

	}

	public void poll() {
		int se = (s + 1) & remain;
		if (se != e) {
			s = se;
			queue[s] = null;
		}
	}

	public T peek() {
		int se = (s + 1) & remain;
		if (se != e) {
			return (T) queue[se];
		} else {
			return null;
		}
	}

	public int size() {
		return (e + remain - s) & remain;
	}

	public boolean hasRemain() {
		return e != s;
	}

	public T last() {
		int es = (e + remain) & remain;
		if (es == s) {
			return null;
		} else {
			return (T) queue[es];
		}
	}

	public void pollLast() {
		int es = (e + remain) & remain;
		if (es != s) {
			queue[es] = null;
			e = es;
		}
	}

	public Bulk getBulk() {
		int se = (s + 1) & remain;
		if (se != e) {
			int length = e - se;
			if (length > 0) {
				return new Bulk(this.queue, se, length);
			} else {
				return new Bulk(this.queue, se, this.queue.length - se);
			}

		} else {
			return null;
		}
	}

	public static class QueueFullException extends Exception {
		private static final long serialVersionUID = -3773853727667039120L;

		public QueueFullException(String message) {
			super(message);
		}
	}

	public class Bulk {
		private T[] queue;
		private int offset;
		private int length;

		public Bulk(T[] queue, int offset, int length) {
			super();
			this.queue = queue;
			this.offset = offset;
			this.length = length;
		}

		public T[] getQueue() {
			return queue;
		}

		public int getOffset() {
			return offset;
		}

		public int getLength() {
			return length;
		}

	}

}
