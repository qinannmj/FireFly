package cn.com.sparkle.firefly;

import java.io.IOException;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.event.listeners.MasterChangePosEventListener;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public class StartUp {
	private final static Logger logger = Logger.getLogger(StartUp.class);

	public static void start(String path) throws Throwable {
		ClassicPaxosServer ps = new ClassicPaxosServer();
		ps.getEventsManager().registerListener(new MasterChangePosEventListener() {
			@Override
			public void masterChange(String address) {
				System.out.println("master change " + address + " !!!!!!!!!!!!!!!!!!!!!");
			}

			@Override
			public void lostPos() {
				System.out.println("lost position !!!!!!!!!!!!!!!!!!!!!");
			}

			@Override
			public void getMasterPos() {
				System.out.println("get position !!!!!!!!!!!!!!!!!!!!!!");

			}
		});
		ps.init(path, new HandlerInterface() {

			@Override
			public void onClientConnect(PaxosSession session) {

			}

			@Override
			public void onClientClose(PaxosSession session) {

			}

			@Override
			public byte[] onLoged(byte[] bytes,int offset,int length) {
				//				logger.info("command:" + new String(bytes));
				
				return "³É¹¦".getBytes();
			}

			@Override
			public void onReceiveLookUp(PaxosSession session, AddRequest request) {

			}

			@Override
			public void onInstanceIdExecuted(long instanceId) {
				try {
					writeExecuteLog(instanceId);
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedChecksumAlgorithm e) {
					e.printStackTrace();
				}
			}

		});
	}

	public static void main(String[] args) throws Throwable {
		start(args[0]);
	}
}
