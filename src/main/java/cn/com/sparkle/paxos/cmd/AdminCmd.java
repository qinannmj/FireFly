package cn.com.sparkle.paxos.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.apache.log4j.PropertyConfigurator;

import cn.com.sparkle.paxos.admin.Commands;
import cn.com.sparkle.paxos.checksum.ChecksumUtil;
import cn.com.sparkle.paxos.client.PaxosClient;
import cn.com.sparkle.paxos.client.PaxosOperater;
import cn.com.sparkle.paxos.cmd.command.Cmd;
import cn.com.sparkle.paxos.cmd.command.CommonCmd;
import cn.com.sparkle.paxos.cmd.command.HelpCmd;
import cn.com.sparkle.paxos.cmd.command.QuitCmd;
import cn.com.sparkle.paxos.cmd.command.SenatorCmd;
import cn.com.sparkle.paxos.model.AdminCommand;

public class AdminCmd {
	private final static HashMap<String, Cmd> commMap = new HashMap<String, Cmd>();
	static{
		commMap.put("help", HelpCmd.DEFAULT);
		commMap.put("?", HelpCmd.DEFAULT);
		commMap.put("quit", QuitCmd.DEFAULT);
		commMap.put("exit", QuitCmd.DEFAULT);
		commMap.put("q", QuitCmd.DEFAULT);
		commMap.put(Commands.CLSTUER_STATE, CommonCmd.DEFAULT);
		commMap.put(Commands.CLSTUER_STATE1, CommonCmd.DEFAULT);
		commMap.put(Commands.RE_ELECTION, CommonCmd.DEFAULT);
		commMap.put(Commands.RE_ELECTION1, CommonCmd.DEFAULT);
		commMap.put(Commands.STATE, CommonCmd.DEFAULT);
		commMap.put(Commands.STATE1, CommonCmd.DEFAULT);
		commMap.put(AdminCommand.ADD_SENATOR, SenatorCmd.DEFAULT);
		commMap.put(AdminCommand.REMOVE_SENATOR, SenatorCmd.DEFAULT);
	}
	
	public static void main(String[] args) throws Throwable {
		if (args.length < 1) {
			System.err.println("cmd serverip1 serverip2 .....");
			System.exit(1);
		}
		String contextPath = AdminCmd.class.getResource("/").getPath();
		PropertyConfigurator.configure(contextPath + "cmdlog4j.properties");
		final PaxosClient client = new PaxosClient(args, contextPath+ "conf10000/service_out_net.prop", "netty", ChecksumUtil.PURE_JAVA_CRC32, 2000, 1,
				999999, false);
		PaxosOperater operater = client.getOperator();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.print(">");
			String line = br.readLine();
			String[] command = line.split(" ");
			Cmd cmd = commMap.get(command[0]);
			if(cmd != null){
				cmd.process(operater,line);
			}else{
				System.out.println("not found command");
			}
		}
	}
}
