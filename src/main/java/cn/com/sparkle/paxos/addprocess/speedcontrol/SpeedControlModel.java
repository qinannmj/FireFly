package cn.com.sparkle.paxos.addprocess.speedcontrol;

import org.apache.log4j.Logger;

import cn.com.sparkle.paxos.Context;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.event.events.SpeedControlEvent;
import cn.com.sparkle.paxos.event.listeners.InstanceExecuteMaxPackageSizeEventListener;
import cn.com.sparkle.paxos.event.listeners.InstancePaxosEventListener;
import cn.com.sparkle.paxos.model.Id;
import cn.com.sparkle.paxos.model.Value;
import cn.com.sparkle.paxos.paxosinstance.InstancePaxosInstance;

public class SpeedControlModel implements InstancePaxosEventListener, InstanceExecuteMaxPackageSizeEventListener {
	private final static Logger logger = Logger.getLogger(SpeedControlModel.class);

	private int curTcpPackageSize;

	private Configuration conf;

	private EventsManager eventsManager;

	public SpeedControlModel(Context context) {
		this.conf = context.getConfiguration();
		this.eventsManager = context.getEventsManager();
		eventsManager.registerListener(this);
	}

	@Override
	public void instanceSuccess(InstancePaxosInstance instance, Value value) {
		int valueSize = 0;
		for (byte[] bs : value.getValue()) {
			valueSize += bs.length;
		}
		if (conf.isDebugLog()) {
			logger.debug("valuesize:" + valueSize);
		}
		if (System.currentTimeMillis() - instance.getStartTime() < conf.getResponseDelay()) {
			int promoteSize = valueSize * 2;

			if (conf.isDebugLog()) {
				logger.debug("up promoteSize :" + promoteSize + " curTcpPackageSize:" + curTcpPackageSize);
			}
			if (promoteSize > curTcpPackageSize) {
				SpeedControlEvent.doSuggestMaxPackageSizeEvent(eventsManager, promoteSize);
			}
		} else {
			int promoteSize = (int) Math.floor(valueSize * 0.75);
			if (conf.isDebugLog()) {
				logger.debug("down promoteSize :" + promoteSize + " curTcpPackageSize:" + curTcpPackageSize);
			}
			if (promoteSize < curTcpPackageSize) {
				SpeedControlEvent.doSuggestMaxPackageSizeEvent(eventsManager, promoteSize);
			}
		}

	}

	@Override
	public void instanceStart(InstancePaxosInstance instance) {
	}

	@Override
	public void maxPackageSizeChange(int curMaxPackageSize) {
		curTcpPackageSize = curMaxPackageSize;
		if (conf.isDebugLog()) {
			logger.debug("maxPackageSizeChange cur value=" + curMaxPackageSize);
		}

	}

	@Override
	public void instanceFail(InstancePaxosInstance instance, Id id, long refuseId, Value value) {
	}

}
