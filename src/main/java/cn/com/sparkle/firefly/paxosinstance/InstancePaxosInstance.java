package cn.com.sparkle.firefly.paxosinstance;

import java.util.LinkedList;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.addprocess.AddRequestPackage;
import cn.com.sparkle.firefly.event.events.InstancePaxosEvent;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.model.Value.ValueType;
import cn.com.sparkle.firefly.paxosinstance.paxossender.PaxosMessageSender;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.util.IdTranslator;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class InstancePaxosInstance extends PaxosInstance {
	private final static Logger logger = Logger.getLogger(InstancePaxosInstance.class);

	private LinkedList<AddRequestPackage> addRequestPackages;

	private Value wantedValue;

	private Id id;

	private long startTime;

	private Context context;

	public InstancePaxosInstance(PaxosMessageSender sender, long instanceId, Id id, LinkedList<AddRequestPackage> addRequestPackages, Context context) {
		super(sender, instanceId, id.getAddress());
		this.context = context;
		this.id = id;
		this.addRequestPackages = addRequestPackages;
		// int count = 0;
		ValueType valueType = ValueType.COMM;
		int byteCount = 0;
		for (AddRequestPackage arp : addRequestPackages) {
			if (arp.isAdmin()) {
				valueType = ValueType.ADMIN;
			}
			byteCount += arp.getValueList().size() * 4 + arp.getValueByteSize();
		}
		wantedValue = new Value(valueType, byteCount);
		for (AddRequestPackage arp : addRequestPackages) {
			for (AddRequest request : arp.getValueList()) {
				wantedValue.add(request.getValue());
			}
		}
	}

	public LinkedList<AddRequestPackage> getAddRequestPackages() {
		return addRequestPackages;
	}

	@Override
	public Value getWantAssginValue() {
		return wantedValue;
	}

	@Override
	public void voteSuccess(Value value) {
		if (logger.isDebugEnabled()) {
			logger.debug("instanceId:" + instanceId + " isWantedValue:" + (value == wantedValue));
		}
		InstancePaxosEvent.doSuccessEvent(context.getEventsManager(), this, value);
		SuccessfulRecord.Builder record = SuccessfulRecord.newBuilder().setHighestVoteNum(IdTranslator.toStoreModelId(this.id));
		try {
			context.getAccountBook().writeSuccessfulRecord(this.instanceId, record, addRequestPackages);
		} catch (Throwable e) {
			logger.error("fatal error", e);
			System.exit(1);
		}
	}

	@Override
	public void instanceFail(long refuseId, Value value) {
		InstancePaxosEvent.doFailEvent(context.getEventsManager(), this, id, refuseId, value);
	}

	@Override
	public Future<Boolean> activate(boolean isWithPreparePhase) {
		startTime = TimeUtil.currentTimeMillis();
		InstancePaxosEvent.doStartEvent(context.getEventsManager(), this);
		return super.activate(isWithPreparePhase);
	}

	public long getStartTime() {
		return startTime;
	}

	@Override
	public Id getId() {
		return this.id;
	}
}
