package cn.com.sparkle.paxos;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.protobuf.ByteString;

import cn.com.sparkle.paxos.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.event.DefaultEventManager;
import cn.com.sparkle.paxos.stablestorage.AccountBook;
import cn.com.sparkle.paxos.stablestorage.ReadRecordCallback;
import cn.com.sparkle.paxos.stablestorage.ReadSuccessReadFilter;
import cn.com.sparkle.paxos.stablestorage.SortedReadCallback;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.paxos.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.paxos.stablestorage.model.SuccessfulRecordWrap;

public class ValidateConsistency {
	public static void main(String[] args) throws InstantiationException, IllegalAccessException, ParserConfigurationException, SAXException, IOException, ClassNotFoundException, UnsupportedChecksumAlgorithm, InterruptedException {
		DefaultEventManager eManager = new DefaultEventManager();
		AccountBook[] abook = new AccountBook[args.length];
		LinkedBlockingQueue<SuccessfulRecordWrap>[] list = new LinkedBlockingQueue[args.length];
		final AtomicBoolean isStop = new AtomicBoolean(false);
		
		for(int i = 0 ; i < args.length ; ++i){
			Configuration configuration = new Configuration(args[i], eManager);
			Context context = new Context(configuration, null, eManager);
			abook[i] = new AccountBook(context,Long.MAX_VALUE);
			list[i] = new LinkedBlockingQueue<SuccessfulRecordWrap>();
			abook[i].init(null);
			final AccountBook b = abook[i];
			final LinkedBlockingQueue<SuccessfulRecordWrap> l = list[i];
			Thread t = new Thread(){
				public void run(){
					try {
						b.readSuccessRecord(0, Long.MAX_VALUE, new ReadSuccessReadFilter(new SortedReadCallback<SuccessfulRecord.Builder>(new ReadRecordCallback<SuccessfulRecord.Builder>() {
							@Override
							public void read(long instanceId, SuccessfulRecord.Builder b) {
								l.add(new SuccessfulRecordWrap(instanceId, b.build(), null));
							}
						}, 0)));
						isStop.set(true);
					} catch (Throwable e) {
						e.printStackTrace();
						System.exit(1);
					} 
				}
			};
			t.start();
		}
		int count = 0;
		while(true){
			boolean stop = isStop.get();
			boolean isEmpty = true;
			for(LinkedBlockingQueue<SuccessfulRecordWrap> l : list){
				isEmpty = isEmpty && l.size() == 0;
			}
			if(!stop || !isEmpty){
				SuccessfulRecordWrap wrap = list[0].take();
				for(int i = 1 ; i < args.length ; ++i){
					SuccessfulRecordWrap tmp = list[i].take();
					if(!eq(tmp.getRecord().getV(),wrap.getRecord().getV())){
						throw new RuntimeException("bad consistency~");
					}
				}
				++count;
					System.out.println("validate count " + count);
			}else{
				break;
			}
		}
		System.out.println("good consistency");
		System.exit(0);
	}
	private static boolean eq(Value v1,Value v2){
		if(v1.getType() != v2.getType()) return false;
		if(v1.getValuesCount()!= v2.getValuesCount()) return false;
		for(int i = 0 ; i < v1.getValuesCount(); ++i){
			ByteString b1 = v1.getValues(i);
			ByteString b2 = v2.getValues(i);
			if(b1.size() != b2.size()){
				return false;
			}
			for(int j = 0 ; j < b1.size() ; ++j){
				if(b1.byteAt(j) != b2.byteAt(j)){
					return false;
				}
			}
		}
		return true;
	}
}