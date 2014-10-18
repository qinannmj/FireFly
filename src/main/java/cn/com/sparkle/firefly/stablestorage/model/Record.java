package cn.com.sparkle.firefly.stablestorage.model;

import java.io.IOException;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.stablestorage.io.RecordFileOut;

public class Record {
	private final static Logger logger = Logger.getLogger(Record.class);
	private RecordHead head;
	private RecordBody body;

	public Record(RecordHead head, RecordBody body) {
		this.head = head;
		this.body = body;
	}

	public void writeToStream(RecordFileOut out, Callable<Object> callable,boolean isSync) throws IOException {
		long pos = head.writeToStream(out, null,false);
		body.writeToStream(out, callable,isSync);
		logger.debug(String.format("write pos %s endpos %s", pos , pos + getSerializeSize()));
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
