package cn.com.sparkle.firefly.net.client.user.callback;

import cn.com.sparkle.firefly.client.Command;
import cn.com.sparkle.firefly.client.CommandAsyncProcessor;
import cn.com.sparkle.firefly.client.CommandAsyncProcessor.EntryNode;

public class AddRequestCallBack {
	private Command c;
	private CommandAsyncProcessor processor;
	private EntryNode e;

	public AddRequestCallBack(Command c, CommandAsyncProcessor processor, EntryNode e) {
		super();
		this.c = c;
		this.processor = processor;
		this.e = e;
	}

	public void fail() {
		processor.wakeup(); //lost a node, the reactor need to wake up to process this situation
	}

	public void call(byte[] response,long instanceId, boolean isLast) {
		EntryNode root = processor.getRoot();
		if (isLast) { // if this is finished ,remove from
						// unfinish list
			if (e.getPrev() == root) {
				synchronized (root) {
					if (e.getPrev() == root) {
						root.setNext(e.getNext());
						if (e.getNext() != null) {
							e.getNext().setPrev(root);
						}
					}
				}
			}
			if (e.getPrev() != root) {
				e.getPrev().setNext(e.getNext());
				if (e.getNext() != null) {
					e.getNext().setPrev(e.getPrev());
				}
			}
			processor.getRunningSize().decrementAndGet();
			processor.wakeup();
		}
		c.finish(response,instanceId);
	}
}
