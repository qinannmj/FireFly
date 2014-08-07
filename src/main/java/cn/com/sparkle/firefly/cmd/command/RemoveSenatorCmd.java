package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.model.AdminCommand;

public class RemoveSenatorCmd extends SenatorCmd {
	public final static RemoveSenatorCmd DEFAULT = new RemoveSenatorCmd();
	@Override
	public String cmdInfo() {
		return AdminCommand.REMOVE_SENATOR + " id: remove the senator from cluster";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{AdminCommand.REMOVE_SENATOR};
	}

}
