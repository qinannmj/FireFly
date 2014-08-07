package cn.com.sparkle.firefly.cmd.command;

import java.util.List;

import cn.com.sparkle.firefly.client.PaxosOperater;

public class HelpCmd implements Cmd {
	List<Cmd> cmdList;
	public HelpCmd(List<Cmd> cmdList){
		this.cmdList = cmdList;
	}
	
	@Override
	public void process(PaxosOperater operater,String command) {
		for(Cmd cmd : cmdList){
			System.out.println(cmd.cmdInfo());
		}
	}
	@Override
	public String cmdInfo() {
		return "help/? : show all command";
	}
	@Override
	public String helpInfo() {
		return "";
	}

	@Override
	public String[] cmd() {
		return new String[]{"help","?"};
	}
}
