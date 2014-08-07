package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.admin.Commands;

public class ReElectionCmd extends CommonCmd {
	public final static ReElectionCmd DEFAULT = new ReElectionCmd();
	@Override
	public String cmdInfo() {
		return Commands.RE_ELECTION + "/"+ Commands.RE_ELECTION1 +" id : elect id to master";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{Commands.RE_ELECTION,Commands.RE_ELECTION1};
	}

}
