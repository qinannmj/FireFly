package cn.com.sparkle.raptor.core.delaycheck;

import cn.com.sparkle.raptor.core.util.TimeUtil;

public abstract class DelayChecked {
	protected long delaytime;
	protected boolean isCycle = false;
	protected long lastCheckTime = TimeUtil.currentTimeMillis();
	protected volatile boolean isRun = false;

	public DelayChecked(long delaytime, boolean isCycle) {
		this.delaytime = delaytime;
		this.isCycle = isCycle;
	}

	public DelayChecked(long delaytime) {
		this(delaytime, false);
	}

	public void needRun() {
		if (delaytime == 0) {
			goToRun();
		} else {
			isRun = true;
		}
	}

	public abstract void goToRun();
}
