package cn.com.sparkle.firefly.cmd.command;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.model.AdminCommand;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;

public abstract class SenatorCmd implements Cmd {

	@Override
	public void process(PaxosOperater operater, String cmd) throws InterruptedException, MasterMayBeLostException {
		String[] command = cmd.split(" ");

		if (command.length != 2) {
			System.out.println("error:" + command[0] + " need more arguments!");
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

		AdminCommand adminCommand = new AdminCommand(command[1], command[0]);
		Future<byte[]> f = operater.add(adminCommand.toBytes(), CommandType.ADMIN_WRITE);
		try {
			String r = new String(f.get(5, TimeUnit.SECONDS));
			System.out.println(r);
		} catch (TimeoutException e) {
			System.out.println("command time out~ please retry!");
		} catch (ExecutionException e) {
			//unreachable
		}
	}
}
