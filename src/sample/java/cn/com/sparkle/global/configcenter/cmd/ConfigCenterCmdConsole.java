package cn.com.sparkle.global.configcenter.cmd;

import cn.com.sparkle.firefly.cmd.AdminCmd;

public class ConfigCenterCmdConsole extends AdminCmd{
	public static void main(String[] args) throws Throwable {
		register(ShowClientCountCmd.DEFAULT);
		AdminCmd.main(args);
	}
	
}
