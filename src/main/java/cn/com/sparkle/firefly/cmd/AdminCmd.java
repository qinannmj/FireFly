package cn.com.sparkle.firefly.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.client.PaxosClient;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.cmd.command.AddSenatorCmd;
import cn.com.sparkle.firefly.cmd.command.ClusterStateCmd;
import cn.com.sparkle.firefly.cmd.command.Cmd;
import cn.com.sparkle.firefly.cmd.command.HelpCmd;
import cn.com.sparkle.firefly.cmd.command.QuitCmd;
import cn.com.sparkle.firefly.cmd.command.ReElectionCmd;
import cn.com.sparkle.firefly.cmd.command.RemoveSenatorCmd;
import cn.com.sparkle.firefly.cmd.command.SenatorChangeRoomCmd;
import cn.com.sparkle.firefly.cmd.command.StateCmd;

public class AdminCmd {
	private final static HashMap<String, Cmd> commMap = new HashMap<String, Cmd>();
	private final static List<Cmd> cmdList = new LinkedList<Cmd>();
	
	static{
		register(QuitCmd.DEFAULT);
		register(ClusterStateCmd.DEFAULT);
		register(ReElectionCmd.DEFAULT);
		register(StateCmd.DEFAULT);
		register(SenatorChangeRoomCmd.DEFAULT);
		register(AddSenatorCmd.DEFAULT);
		register(RemoveSenatorCmd.DEFAULT);
		
	}
	public static void register(Cmd cmd){
		for(String name:cmd.cmd()){
			Cmd old = commMap.put(name, cmd);
			cmdList.remove(old);
			if(old!=null){
				throw new RuntimeException(String.format("repeated name of cmd!name:%s", name));
			}
		}
		cmdList.add(cmd);
	}
	public static void replace(Cmd cmd){
		for(String name:cmd.cmd()){
			Cmd old = commMap.put(name, cmd);
			cmdList.remove(old);
		}
		cmdList.add(cmd);
	}
	public static void remove(Cmd cmd){
		for(String name:cmd.cmd()){
			Cmd old = commMap.get(name);
			if(old != null){
				cmdList.remove(old);
			}
		}
	}
	
	public static void main(String[] args) throws Throwable {
		register(new HelpCmd(cmdList));
		if (args.length < 1) {
			System.err.println("cmd serverip1 serverip2 .....");
			System.exit(1);
		}
		String contextPath = AdminCmd.class.getResource("/").getPath();
		PropertyConfigurator.configure(contextPath + "cmdlog4j.properties");
		final PaxosClient client = new PaxosClient(args, contextPath+ "conf10000/service_out_net.prop", "netty", ChecksumUtil.PURE_JAVA_CRC32, 2000, 1,
				999999);
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
