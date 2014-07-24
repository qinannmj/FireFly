package cn.com.sparkle.paxos.cmd.command;

import cn.com.sparkle.paxos.client.PaxosOperater;

public class QuitCmd implements Cmd{
	public final static QuitCmd DEFAULT = new QuitCmd();
	public void process(PaxosOperater operater,String command) {
		System.exit(0);
	};
}
