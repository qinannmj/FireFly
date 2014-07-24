package cn.com.sparkle.paxos.stablestorage;

import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord.Builder;

public class ReadSuccessReadFilter extends ReadSuccessRecordCallback {
	ReadRecordCallback<SuccessfulRecord.Builder> callback;

	public ReadSuccessReadFilter(ReadRecordCallback<SuccessfulRecord.Builder> callback) {
		this.callback = callback;
	}

	@Override
	public void readSuccess(long instanceId, Builder successfulRecordBuilder) {
		callback.read(instanceId, successfulRecordBuilder);
	}

}
