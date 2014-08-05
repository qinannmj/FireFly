package cn.com.sparkle.firefly.addprocess.speedcontrol;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.SpeedControlEvent;
import cn.com.sparkle.firefly.event.listeners.InstanceExecuteMaxPackageSizeEventListener;
import cn.com.sparkle.firefly.event.listeners.InstancePaxosEventListener;
import cn.com.sparkle.firefly.model.Id;
import cn.com.sparkle.firefly.model.Value;
import cn.com.sparkle.firefly.paxosinstance.InstancePaxosInstance;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class SpeedControlModel implements InstancePaxosEventListener, InstanceExecuteMaxPackageSizeEventListener {
	private final static Logger logger = Logger.getLogger(SpeedControlModel.class);

	private int curTcpPackageSize;

	private Configuration conf;

	private EventsManager eventsManager;
	
	private long lastSuccessTime = -1;

	public SpeedControlModel(Context context) {
		this.conf = context.getConfiguration();
		this.eventsManager = context.getEventsManager();
		eventsManager.registerListener(this);
	}

	@Override
	public void instanceSuccess(InstancePaxosInstance instance, Value value) {
		int valueSize = value.length();
		if (conf.isDebugLog()) {
			logger.debug(String.format("valuesize:%s instanceStartTime:%s lastSuccessTime:%s",valueSize,instance.getStartTime(),lastSuccessTime) );
		}
		long curRealStartTime = Math.max(instance.getStartTime(), lastSuccessTime);
		if (TimeUtil.currentTimeMillis() - curRealStartTime < conf.getResponseDelay()) {
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
//		lastSuccessTime = TimeUtil.currentTimeMillis();
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
