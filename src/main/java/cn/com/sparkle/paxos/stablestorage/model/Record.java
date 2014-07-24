package cn.com.sparkle.paxos.stablestorage.model;

import java.io.IOException;
import java.util.concurrent.Callable;

import cn.com.sparkle.paxos.stablestorage.io.BufferedFileOut;

public class Record {
	private RecordHead head;
	private RecordBody body;

	public Record(RecordHead head, RecordBody body) {
		this.head = head;
		this.body = body;
	}

	public void writeToStream(BufferedFileOut out, Callable<Object> callable) throws IOException {
		head.writeToStream(out, null);
		body.writeToStream(out, callable);
	}

	public RecordHead getHead() {
		return head;
	}

	public RecordBody getBody() {
		return body;
	}

}
