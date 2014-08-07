package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.model.AdminCommand;

public class AddSenatorCmd extends SenatorCmd {
	public final static AddSenatorCmd DEFAULT = new AddSenatorCmd();
	@Override
	public String cmdInfo() {
		return AdminCommand.ADD_SENATOR + " id: add a senator to cluster";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{AdminCommand.ADD_SENATOR};
	}

}
