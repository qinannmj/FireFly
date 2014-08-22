package cn.com.sparkle.firefly.stablestorage.model;

import java.io.IOException;
import java.util.concurrent.Callable;

import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;

public class Record {
	private RecordHead head;
	private RecordBody body;

	public Record(RecordHead head, RecordBody body) {
		this.head = head;
		this.body = body;
	}

	public void writeToStream(RecordFileOut out, Callable<Object> callable,boolean isSync) throws IOException {
		head.writeToStream(out, null,false);
		body.writeToStream(out, callable,isSync);
	}

	public RecordHead getHead() {
		return head;
	}

	public RecordBody getBody() {
		return body;
	}
	
	public int getSerializeSize(){
		return this.head.getSerializeSize() + this.body.getSerializeSize();
	}
}
