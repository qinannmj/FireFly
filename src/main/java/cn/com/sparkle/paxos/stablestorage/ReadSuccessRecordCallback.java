package cn.com.sparkle.paxos.stablestorage;

import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;

import com.google.protobuf.GeneratedMessage.Builder;

@SuppressWarnings("rawtypes")
public abstract class ReadSuccessRecordCallback implements ReadRecordCallback<Builder<? extends Builder>> {
	public abstract void readSuccess(long instanceId, SuccessfulRecord.Builder successfulRecordBuilder);

	@Override
	public final void read(long instanceId, Builder<? extends Builder> b) {
		if (b instanceof SuccessfulRecord.Builder) {
			readSuccess(instanceId, (SuccessfulRecord.Builder) b);
		}
	}
}
