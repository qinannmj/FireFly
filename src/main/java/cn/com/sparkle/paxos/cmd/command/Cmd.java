package cn.com.sparkle.paxos.cmd.command;

import cn.com.sparkle.paxos.client.MasterMayBeLostException;
import cn.com.sparkle.paxos.client.PaxosOperater;

public interface Cmd {
	public void process(PaxosOperater operater,String cmd) throws InterruptedException, MasterMayBeLostException;
}
