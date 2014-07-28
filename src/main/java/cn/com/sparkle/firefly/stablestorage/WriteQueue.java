package cn.com.sparkle.firefly.stablestorage;

import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

public class WriteQueue<Tag, Element, MyNode extends WriteQueue.Node<Tag, Element>> {
	@SuppressWarnings("unused")
	private final static Logger logger = Logger.getLogger(WriteQueue.class);
	private ReentrantLock lock = new ReentrantLock();
	private HashMap<Tag, MyNode> lastNodeOfTag = new HashMap<Tag, MyNode>();
	private Node<Tag, Element> root;
	private Condition condition = lock.newCondition();
	private int size = 0;

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
		public void setTag(Tag tag){
			this.tag = tag;
		}
		public abstract boolean canGet();
	}

	public WriteQueue() {
		root = new Node<Tag, Element>(null, null) {
			@Override
			public boolean canGet() {
				return false;
			}
		};
		root.prev = root;
		root.next = root;
	}

	public void push(MyNode n) {
		try {
			lock.lock();
			n.next = root.prev.next;
			n.prev = root.prev;
			root.prev.next = n;
			root.prev = n;
			lastNodeOfTag.put(n.tag, n);
			++size;
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	public MyNode take() throws InterruptedException {
		try {
			lock.lock();
			while (root.next == root) {
				condition.await();
			}
			@SuppressWarnings("unchecked")
			MyNode n = (MyNode) root.next;
			root.next = n.next;
			n.next.prev = root;
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
	public int size(){
		try{
			lock.lock();
			return size;
		}finally{
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
			a.push(new MyNode(i, ""));
		}
		a.push(a.getLastNodeOfTag(500));

		for (int i = 0; i < 1000; i++) {
			a.take();
		}
	}
}
