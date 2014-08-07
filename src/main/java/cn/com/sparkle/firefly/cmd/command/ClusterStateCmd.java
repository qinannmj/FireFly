package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.admin.Commands;

public class ClusterStateCmd extends CommonCmd {
	public final static ClusterStateCmd DEFAULT = new ClusterStateCmd();

	@Override
	public String cmdInfo() {
		return Commands.CLSTUER_STATE + "/" + Commands.CLSTUER_STATE1 + " : show the state of cluster";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[] { Commands.CLSTUER_STATE, Commands.CLSTUER_STATE1 };
	}

}
