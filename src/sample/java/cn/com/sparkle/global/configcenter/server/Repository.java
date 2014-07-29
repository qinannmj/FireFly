package cn.com.sparkle.global.configcenter.server;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Entity;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Value;

import com.google.protobuf.InvalidProtocolBufferException;

public class Repository {
	private Logger logger = Logger.getLogger(Repository.class);
	
	MappedByteBuffer buff = null;
	File curFile = null;
	public Repository(String path) throws IOException, NoSuchAlgorithmException{
		//初始化文件系统
		File dir = new File(path + "/repo");
		if(dir.exists() == false){
			dir.mkdirs();
		}
		
		File[] files = (new File(path + "/repo")).listFiles();
		Comparator<File> comparator = new Comparator<File>() {
			public int compare(File o1, File o2) {
				String o1n = o1.getName().split("-")[1];
				String o2n = o2.getName().split("-")[1];
				return Long.valueOf(o2n).compareTo(
						Long.valueOf(o1n));
			}
		};
		Arrays.sort(files,comparator);
		byte[] data = new byte[10 * 1024 * 1024];
		//校验磁盘有效性，使用最后一个有效的快照
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		for(File f : files){
			RandomAccessFile r = new RandomAccessFile(f, "r");
			buff = r.getChannel().map(MapMode.READ_ONLY, 0, f.length());
			//checksum
			boolean isSafe = true;
			long readSize = 0;
			while(readSize<f.length() - 16){
				int canReadSize = (int)Math.min(f.length() - 16 - readSize, data.length);
				buff.get(data, 0, canReadSize);
				md5.update(data,0,canReadSize);
				readSize += canReadSize;
			}
			byte[] checksum = md5.digest();
			for(byte b: checksum){
				if(b != buff.get()){
					isSafe = false;
					break;
				}
			}
			if(isSafe){
				curFile = f;
				break;
			}
		}
	}
	
	public long initMemory(Map<String, Value> map){
		//加载磁盘文件进入内存
		if(curFile != null){
			buff.position(0);
			while(buff.position() + 16 < buff.limit()){
				int packageSize = buff.getInt();
				byte[] bytes = new byte[packageSize];
				buff.get(bytes);
				try {
					Entity e = Entity.parseFrom(bytes);
					map.put(e.getKey(), e.getValue());
				} catch (InvalidProtocolBufferException e) {
					logger.error("fatal error", e);
				}
			}
			return Long.valueOf(curFile.getName().split("-")[1]);
		}
		return -1;
	}
	public static void saveToDisk(Map<String,Value> map,long instanceId,String path) throws IOException, NoSuchAlgorithmException{
		//dump内存为快照文件
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		File f = new File(path+ "/repo/snapshot-" + instanceId + "-" + sdf.format(new Date()));
		DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		byte[] intbyte = new byte[4];
		for(Entry<String, Value> e : map.entrySet()){
			Entity entity = Entity.newBuilder().setKey(e.getKey()).setValue(e.getValue()).build();
			int size = entity.getSerializedSize();
			intbyte[0] = (byte) ((size >>> 24) & 0xFF);
			intbyte[1] = (byte) ((size >>> 16) & 0xFF);
			intbyte[2] = (byte) ((size >>> 8) & 0xFF);
			intbyte[3] = (byte) ((size >>> 0) & 0xFF);
			byte[] data = entity.toByteArray();
			
			out.write(intbyte);
			out.write(data);
			md5.update(intbyte);
			md5.update(data);
		}
		out.write(md5.digest());
		out.close();
	}
}
