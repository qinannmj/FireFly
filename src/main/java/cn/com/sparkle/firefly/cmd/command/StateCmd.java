package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.admin.Commands;

public class StateCmd extends CommonCmd {
	public final static StateCmd DEFAULT = new StateCmd();
	@Override
	public String cmdInfo() {
		return Commands.STATE + "/" + Commands.STATE1 + " id : show a node's status of this cluster.";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{Commands.STATE,Commands.STATE1};
	}

}
