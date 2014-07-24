package cn.com.sparkle.paxos.stablestorage.util;

import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel;

public class IdTranslator {
	public static StoreModel.Id.Builder toStoreModelId(Id id) {
		StoreModel.Id.Builder builder = StoreModel.Id.newBuilder();
		builder.setAddress(id.getAddress());
		builder.setIncreaseId(id.getIncreaseId());
		return builder;
	}

	public static Id toId(StoreModel.IdOrBuilder sid) {
		Id id = new Id(sid.getAddress(), sid.getIncreaseId());
		return id;
	}
}
