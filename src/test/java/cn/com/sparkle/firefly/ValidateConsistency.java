package cn.com.sparkle.firefly;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.google.protobuf.ByteString;

import cn.com.sparkle.firefly.Context;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.event.DefaultEventManager;
import cn.com.sparkle.firefly.model.Value.IterElement;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.stablestorage.ReadRecordCallback;
import cn.com.sparkle.firefly.stablestorage.ReadSuccessReadFilter;
import cn.com.sparkle.firefly.stablestorage.SortedReadCallback;
import cn.com.sparkle.firefly.stablestorage.model.SuccessfulRecordWrap;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.SuccessfulRecord;
import cn.com.sparkle.firefly.stablestorage.model.StoreModel.Value;
import cn.com.sparkle.firefly.stablestorage.util.ValueTranslator;

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
		cn.com.sparkle.firefly.model.Value mv1 = ValueTranslator.toValue(v1);
		cn.com.sparkle.firefly.model.Value mv2 = ValueTranslator.toValue(v2);
		if(mv1.length() != mv2.length()){
			return false;
		}else{
			for(int i = 0 ; i < mv1.length() ; ++i){
				if(mv1.getValuebytes()[i] != mv2.getValuebytes()[i]){
					return false;
				}
			}
		}
		return true;
	}
}