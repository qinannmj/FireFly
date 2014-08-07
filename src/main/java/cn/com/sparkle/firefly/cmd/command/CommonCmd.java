package cn.com.sparkle.firefly.cmd.command;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;

public abstract class CommonCmd implements Cmd{
	public void process(PaxosOperater operater, String line) throws InterruptedException, MasterMayBeLostException {
		Future<byte[]> f = operater.add(line.getBytes(), CommandType.ADMIN_READ_TRANSPORT_MASTER);
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
