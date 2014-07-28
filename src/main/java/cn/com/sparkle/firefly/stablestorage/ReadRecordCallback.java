package cn.com.sparkle.firefly.stablestorage;

import com.google.protobuf.GeneratedMessage.Builder;
@SuppressWarnings("rawtypes")
public interface ReadRecordCallback<T extends Builder<? extends Builder>> {
	
	public void read(long instanceId, T b);
}
