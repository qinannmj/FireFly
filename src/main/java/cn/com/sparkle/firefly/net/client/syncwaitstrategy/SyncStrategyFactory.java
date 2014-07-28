package cn.com.sparkle.firefly.net.client.syncwaitstrategy;

import cn.com.sparkle.firefly.config.Configuration;

public class SyncStrategyFactory {
	public static WaitStrategy build(Configuration conf) {
		String strategyName = conf.getSessionSuccessSyncMaxMemStrategy();
		if ("closeConnectionAfterWaitTimeStrategy".equals(strategyName)) {
			return new CloseConnectionAfterWaitTimeStrategy(conf.getSessionSuccessSyncMaxMemWaitTime());
		} else if ("waitStrategy".equals(strategyName)) {
			return new KeepWaitStrategy();
		} else {
			throw new RuntimeException("can't found session-success-sync-max-mem-strategy : " + strategyName);
		}
	}
}
