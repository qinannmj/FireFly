package cn.com.sparkle.paxos.net.client.system.callback;

import java.io.IOException;
import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.future.SystemFuture;
import cn.com.sparkle.paxos.stablestorage.AccountBook;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;

public class CatchUpCallBack {

	private final static Logger logger = Logger.getLogger(CatchUpCallBack.class);

	private SystemFuture<Boolean> future = new SystemFuture<Boolean>();

	private Configuration conf;

	private AccountBook aBook;

	private int studyNum = 0;

	public CatchUpCallBack(Configuration conf, AccountBook aBook) {
		this.conf = conf;
		this.aBook = aBook;
	}

	public void fail() {
		if(conf.isDebugLog()){
			logger.debug("study failed!");
		}
		future.set(false);
	}

	public void callback(long instanceId, SuccessfulRecord record) {
		++studyNum;
		try {
			aBook.writeSuccessfulRecord(instanceId, record.toBuilder(), null);
		} catch (IOException e) {
			logger.error("fatal error:", e);
		} catch (UnsupportedChecksumAlgorithm e) {
			logger.error("unexcepted error:", e);
		}
	}

	public void finish() {
		if (conf.isDebugLog()) {
			logger.debug("study " + studyNum + " successful records");
		}
		future.set(true);
	}

	public void waitFinish() throws InterruptedException {
		future.get();
	}
}
