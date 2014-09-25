package cn.com.sparkle.firefly.stablestorage;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

public class WriteQueue<Tag, Element, MyNode extends WriteQueue.Node<Tag, Element>> {
	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(WriteQueue.class);
	private final static int LOW_IDLE = 4;
	private ReentrantLock lock = new ReentrantLock();
	private HashMap<Tag, MyNode> lastNodeOfTag = new HashMap<Tag, MyNode>();
	private Node<Tag, Element> highPrior;
	private Node<Tag, Element> lowPrior;
	private Condition condition = lock.newCondition();
	private int size = 0;

	private int lowPriorIdle = LOW_IDLE; //for 1:LOW_IDLE low write : high write

	public static abstract class Node<Tag, Element> {
		Element element;
		Tag tag;
		protected Node<Tag, Element> prev = null;
		protected Node<Tag, Element> next = null;

		public Node(Tag tag, Element element) {
			super();
			this.element = element;
			this.tag = tag;
		}

		public Element getElement() {
			return element;
		}

		public Tag getTag() {
			return tag;
		}

		public void setTag(Tag tag) {
			this.tag = tag;
		}

		public abstract boolean canGet();
	}

	public WriteQueue() {
		highPrior = new Node<Tag, Element>(null, null) {
			@Override
			public boolean canGet() {
				return false;
			}
		};
		highPrior.prev = highPrior;
		highPrior.next = highPrior;

		lowPrior = new Node<Tag, Element>(null, null) {
			@Override
			public boolean canGet() {
				return false;
			}
		};
		lowPrior.prev = lowPrior;
		lowPrior.next = lowPrior;
	}

	public void push(MyNode n, boolean isHighPrior) {
		try {
			lock.lock();
			if (isHighPrior) {
				n.next = highPrior.prev.next;
				n.prev = highPrior.prev;
				highPrior.prev.next = n;
				highPrior.prev = n;
				lastNodeOfTag.put(n.tag, n);
			} else {
				n.next = lowPrior.prev.next;
				n.prev = lowPrior.prev;
				lowPrior.prev.next = n;
				lowPrior.prev = n;
				lastNodeOfTag.put(n.tag, n);
			}
			++size;
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public MyNode take(int timeout) throws InterruptedException {
		try {
			lock.lock();
			while (highPrior.next == highPrior && (lowPrior.next == lowPrior || lowPriorIdle != 0)) {
//			while (highPrior.next == highPrior && lowPrior.next == lowPrior) {
				condition.await(timeout, TimeUnit.MILLISECONDS);
				if (highPrior.next == highPrior && lowPrior.next == lowPrior) {
					return null;
				} 
				else if (highPrior.next == highPrior && lowPriorIdle != 0) {
					--lowPriorIdle;
				}
			}
			MyNode n;
			if(lowPrior.next != lowPrior && lowPriorIdle == 0){
				n = (MyNode) lowPrior.next;
				lowPrior.next = n.next;
				n.next.prev = lowPrior;
				if(highPrior.next != highPrior){
					lowPriorIdle = LOW_IDLE;
				}
			}else{
				n = (MyNode) highPrior.next;
				highPrior.next = n.next;
				n.next.prev = highPrior;
				if(lowPriorIdle != 0){
					--lowPriorIdle;
				}
			}
//			
//			if (highPrior.next != highPrior && ()) {
//				n = (MyNode) highPrior.next;
//				highPrior.next = n.next;
//				n.next.prev = highPrior;
//				if(lowPriorIdle == 1){
//					lowPriorIdle = LOW_IDLE;
//				}else{
//					--lowPriorIdle;
//				}
//			} else {
//				n = (MyNode) lowPrior.next;
//				lowPrior.next = n.next;
//				n.next.prev = lowPrior;
//				
//			}
			lastNodeOfTag.remove(n.tag);
			--size;
			return n;
		} finally {
			lock.unlock();
		}
	}

	public MyNode getLastNodeOfTag(Tag tag) {
		try {
			lock.lock();
			MyNode n = (MyNode) lastNodeOfTag.get(tag);
			if (n == null || !n.canGet()) {
				return null;
			}
			n.next.prev = n.prev;
			n.prev.next = n.next;
			lastNodeOfTag.remove(tag);
			--size;
			return n;
		} finally {
			lock.unlock();
		}
	}

	public int size() {
		try {
			lock.lock();
			return size;
		} finally {
			lock.unlock();
		}

	}

	public static void main(String[] args) throws InterruptedException {
		class MyNode extends WriteQueue.Node<Integer, String> {
			public MyNode(Integer tag, String element) {
				super(tag, element);
			}

			@Override
			public boolean canGet() {
				return true;
			}
		}
		WriteQueue<Integer, String, MyNode> a = new WriteQueue<Integer, String, MyNode>();
		for (int i = 0; i < 1000; i++) {
			a.push(new MyNode(i, ""), true);
		}
		a.push(a.getLastNodeOfTag(500), true);

	}
}
