package cn.com.sparkle.global.configcenter.server;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import cn.com.sparkle.firefly.ClassicPaxosServer;
import cn.com.sparkle.firefly.admin.processors.AdminProcessor;
import cn.com.sparkle.global.configcenter.message.ProtobufMessages.Value;
import cn.com.sparkle.global.configcenter.server.admin.ShowClientCountProcessor;

public class ConfigServer {
	public static void start(String path) throws Throwable {
		ConcurrentHashMap<String, Value> map = new ConcurrentHashMap<String, Value>(
				10000);
		Repository repository = new Repository(path);
		long instanceId = repository.initMemory(map);

		LinkedList<AdminProcessor> list = new LinkedList<AdminProcessor>();
		
		ClassicPaxosServer ps = new ClassicPaxosServer(instanceId + 1);
		ConfigServerHandler handler = new ConfigServerHandler(map, path);
		
		list.add(new ShowClientCountProcessor(handler));
		ps.init(path, handler , list);
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.out.println("you need a dir of configuration!");
		}
	}
}
