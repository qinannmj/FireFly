package cn.com.sparkle.firefly.cmd.command;

import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosOperater;

public class SenatorChangeRoomCmd extends CommonCmd {
	public final static SenatorChangeRoomCmd DEFAULT = new SenatorChangeRoomCmd();

	@Override
	public void process(PaxosOperater operater, String cmd) throws InterruptedException, MasterMayBeLostException {
		String[] command = cmd.split(" ");
		
			if (command.length != 3) {
				System.out.println("error:" + command[0] + " need more arguments! command format : chr address room");
				return;
			}
			String[] args = command[1].split(":");
			if (args.length != 2) {
				System.out.println("error: id format [ip|host:systemport]");
				return;
			}
			try {
				Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("error:systemport is not number");
			}
			super.process(operater, cmd);
	}
}
