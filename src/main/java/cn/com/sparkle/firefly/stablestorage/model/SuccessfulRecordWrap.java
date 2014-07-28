package cn.com.sparkle.firefly.stablestorage.model;

import java.util.LinkedList;

import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;

public class SuccessfulRecordWrap implements Comparable<SuccessfulRecordWrap> {
	private long instanceId;
	private SuccessfulRecord record;
	private LinkedList<AddRequestPackage> addRequestPackages;

	public SuccessfulRecordWrap(long instanceId, SuccessfulRecord record, LinkedList<AddRequestPackage> addRequestPackages) {
		super();
		this.instanceId = instanceId;
		this.record = record;
		this.addRequestPackages = addRequestPackages;
	}

	public long getInstanceId() {
		return instanceId;
	}

	public SuccessfulRecord getRecord() {
		return record;
	}

	public LinkedList<AddRequestPackage> getAddRequestPackages() {
		return addRequestPackages;
	}

	@Override
	public int compareTo(SuccessfulRecordWrap o) {
		if (getInstanceId() == o.getInstanceId())
			return 0;
		return getInstanceId() > o.getInstanceId() ? 1 : -1;
	}

	@Override
	public int hashCode() {
		return (int) (getInstanceId() ^ (getInstanceId() >>> 32));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SuccessfulRecordWrap) {
			SuccessfulRecordWrap sfr = (SuccessfulRecordWrap) obj;
			return sfr.getInstanceId() == getInstanceId();
		} else
			return false;
	}
}
