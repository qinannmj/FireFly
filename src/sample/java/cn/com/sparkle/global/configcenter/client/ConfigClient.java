package cn.com.sparkle.global.configcenter.client;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;
import org.iq80.snappy.Snappy;

import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.client.MasterMayBeLostException;
import cn.com.sparkle.firefly.client.PaxosClient;
import cn.com.sparkle.firefly.client.PaxosOperater;
import cn.com.sparkle.firefly.model.AddRequest.CommandType;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Add;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Messages;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Value;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Watch;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

public class ConfigClient {
	private final static Logger logger = Logger.getLogger(ConfigClient.class);
	private PaxosClient paxosClient;
	private final PaxosOperater operator;
	private ConcurrentHashMap<String, LocalValue> localCache = new ConcurrentHashMap<String, LocalValue>();

	public ConfigClient(String[] server, String path) throws Throwable {
		paxosClient = new PaxosClient(server, path, "raptor", ChecksumUtil.PURE_JAVA_CRC32, 2000, 3, 2, false);
		operator = paxosClient.getOperator();
	}

	public String get(final String key) throws InterruptedException, TimeoutException, MasterMayBeLostException {
		LocalValue v = localCache.get(key);
		if (v == null) {
			synchronized (this) {
				v = localCache.get(key);
				if (v != null) {
					return v.getValue();
				} else {
					Watch.Builder b = Watch.newBuilder().setKey(key).setVersion(-1);
					Messages.Builder m = Messages.newBuilder().setWatch(b);
					// 发送注册watch命令
					Future<byte[]> future = operator.add(m.build().toByteArray(), 5000, CommandType.USER_CONSISTENTLY_READ, new PaxosOperater.CallBack() {

						@Override
						public void callBack(byte[] bytes) {
							Value value = null;
							try {
								value = Value.parseFrom(bytes);
							} catch (InvalidProtocolBufferException e) {
								logger.error("fatal error", e);
							}
							byte[] v = value.getValue().toByteArray();
							if (v.length != 0) {
								v = Snappy.uncompress(v, 0, v.length);// 解压
								LocalValue lv;
								try {
									String content = new String(v, "utf-8");
									lv = new LocalValue(content, value.getVersion());
									localCache.put(key, lv);
									logger.debug("receive version:" + value.getVersion() + " key:" + key + " content:" + content);
								} catch (UnsupportedEncodingException e) {
									logger.error("fatal", e);
								}
							} else {
								logger.debug("receive version:" + value.getVersion());
							}

						}
					});
					localCache.putIfAbsent(key, new LocalValue(null, -1));//将阻止再次watch
					// 获取第一次watch结果
					try {
						future.get(10, TimeUnit.SECONDS);
						v = localCache.get(key);
					} catch (ExecutionException e) {
						logger.error("fatal error", e);
					} catch (TimeoutException e) {
						throw new MasterMayBeLostException();
					}
				}
			}
		}
		if (v == null) {
			return null;
		} else {
			return v.getValue();
		}
	}

	public boolean set(final String key, final String value) throws InterruptedException, ExecutionException, UnsupportedEncodingException,
			MasterMayBeLostException {
		byte[] b = value.getBytes("utf-8");
		b = Snappy.compress(b);// 压缩
		Add add = Add.newBuilder().setKey(key).setValue(ByteString.copyFrom(b)).build();
		Future<byte[]> future = operator.add(add.toByteArray(), CommandType.USER_WRITE);
		try {
			byte[] response = future.get(5, TimeUnit.SECONDS);
			if (response[0] == 1) {
				return true;
			} else {
				return false;
			}
		} catch (TimeoutException e) {
			throw new MasterMayBeLostException();
		}
	}
}
