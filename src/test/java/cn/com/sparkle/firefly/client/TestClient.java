package cn.com.sparkle.firefly.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosClient;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.raptor.core.util.TimeUtil;

public class TestClient {
	private final static Logger logger = Logger.getLogger(TestClient.class);

	public static void main(String[] args) throws Throwable {
		PropertyConfigurator.configure("target/classes/client/log4j.properties");
		final AtomicLong totalCost= new AtomicLong();
		final AtomicInteger[] rtArray = new AtomicInteger[11];
		for(int i = 0 ;i < rtArray.length ; ++i){
			rtArray[i] = new AtomicInteger();
		}
		
				
		//		PaxosOperater oper = client.getOperator();

		final int size = 150000;
		int threadSize = 10;

		String type = args.length > 0 ?args[0] : "raptor";
		int cycle = args.length > 1 ? Integer.parseInt(args[1]):1;
		String[] address = {"127.0.0.1:10001", "127.0.0.1:8001","127.0.0.1:9001",  "127.0.0.1:12001","127.0.0.1:10001" };
		
		if(args.length > 2){
			address = new String[args.length - 2];
			for(int i = 2 ; i < args.length ; ++i){
				address[i - 2] = args[i];
			}
		}
		
		final PaxosClient client = new PaxosClient(address, "target/classes/service_out_net.prop", type,
				ChecksumUtil.NO_CHECKSUM, 2000, 5, 1,10 * 1024 * 1024,true);
		
		

		logger.info("ø™ º≤‚ ‘");
		String sample = "dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd2131231231123   ";
//		String sample="1";
		String a = "";
		for (int i = 0; i < cycle; ++i) {
			a += sample;
		}
		final String sample1 = a;
		Thread.sleep(3000);
		long ct = System.currentTimeMillis();
		System.out.println(a.getBytes().length);
		final CountDownLatch count = new CountDownLatch(threadSize);
		for (int i = 0; i < threadSize; ++i) {
			Thread t = new Thread() {
				public void run() {
					try {
						PaxosOperater oper = client.getOperator();
						for (int i = 0; i < size; i++) {
							
							Future<byte[]> f = oper.add(sample1.getBytes(), 0, CommandType.USER_WRITE, new PaxosOperater.CallBack() {
								private long st = TimeUtil.currentTimeMillis();
								public void callBack(byte[] response) {
									long rt = TimeUtil.currentTimeMillis() - st;
									long flag = rt / 50;
									if(flag < 10){
										rtArray[(int)flag].incrementAndGet();
									}else{
										rtArray[10].incrementAndGet();
									}
									totalCost.addAndGet(rt);
								}
							});
							//							Future<AddResponse> f = oper.add(("" + i).getBytes(),0,true,null);
							//							Future<AddResponse> f = oper.add(new byte[128],0);
							//							AddResponse response = f.get();
							//							System.out.println(new String(response.getResult().toByteArray()));
							//							logger.info("ÃÌº”√¸¡Ócost:" + (System.currentTimeMillis() - ct));
							//							ct = System.currentTimeMillis();
							//							Thread.sleep(100);
//							Thread.sleep(100);
							f.get();
						}
						oper.waitAllFinish(6000000);
					} catch (InterruptedException e) {
						logger.error("", e);
					} catch (MasterMayBeLostException e) {
						logger.error("", e);
					} catch (Throwable e) {
						e.printStackTrace();
					} finally {
						count.countDown();
					}
				}
			};
			t.start();
		}
		
		Thread tpsThread = new Thread(){
			public void run(){
				long lastRequest = 0;
				while(true){
					long totalRequest = 1;
					try {
						count.await(1,TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					StringBuffer sb = new StringBuffer();
					sb.append("\r\n");
					for(int i = 0 ; i < 10 ; i++){
						sb.append("rt[").append(i * 50).append("] :").append(rtArray[i].get()).append("\r\n");
						totalRequest += rtArray[i].get();
					}
					sb.append("rt[>500ms] :").append(rtArray[10].get()).append("\r\n");
					totalRequest += rtArray[10].get();
					sb.append("average rt:").append(totalCost.get() / totalRequest ).append("(").append(totalCost.get()).append("/").append(totalRequest).append(")");
					sb.append("\r\n").append("qps:").append((totalRequest -lastRequest));
					
					lastRequest = totalRequest;
					logger.debug(sb.toString());
					if(count.getCount() == 0){
						break;
					}
				}
			}
		};
		tpsThread.start();
		
		
		count.await();
		System.out.println(size * threadSize * 1000L / (System.currentTimeMillis() - ct) + " tps");
		System.exit(0);
	}
}
