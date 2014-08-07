package cn.com.sparkle.global.configcenter.cmd;

import cn.com.sparkle.firefly.cmd.command.CommonCmd;

public class ShowClientCountCmd extends CommonCmd {
	public final static ShowClientCountCmd DEFAULT = new ShowClientCountCmd();
	@Override
	public String cmdInfo() {
		return "showcn id : show the num of clients that connected to the node named id.";
	}

	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{"showcn"};
	}

}
