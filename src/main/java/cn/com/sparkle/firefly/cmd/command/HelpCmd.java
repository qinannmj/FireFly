package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.admin.Commands;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.model.AdminCommand;

public class HelpCmd implements Cmd {
	public final static HelpCmd DEFAULT = new HelpCmd();
	@Override
	public void process(PaxosOperater operater,String command) {
		System.out.println(Commands.STATE + "/" + Commands.STATE1 + " id : show a node's status of this cluster.");
		System.out.println(Commands.RE_ELECTION + "/"+ Commands.RE_ELECTION1 +" id : elect id to master");
		System.out.println(Commands.CLSTUER_STATE + "/" + Commands.CLSTUER_STATE1 +" : show the state of cluster");
		System.out.println(AdminCommand.ADD_SENATOR + " id room: add a senator to cluster");
		System.out.println(AdminCommand.REMOVE_SENATOR + " id: remove the senator from cluster");
		
		
		System.out.println("help/? : show all command");
		System.out.println("exit/quit/q : quit from cmd client");
	}
}
