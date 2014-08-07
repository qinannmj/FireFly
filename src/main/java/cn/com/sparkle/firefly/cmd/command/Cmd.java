package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosOperater;

public interface Cmd {
	public void process(PaxosOperater operater,String cmd) throws InterruptedException, MasterMayBeLostException;
	public String cmdInfo();
	public String helpInfo();
	public String[] cmd();
}
