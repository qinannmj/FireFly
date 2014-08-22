package cn.com.sparkle.firefly.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import cn.com.sparkle.firefly.Constants;
import cn.com.sparkle.firefly.checksum.ChecksumUtil;
import cn.com.sparkle.firefly.checksum.ChecksumUtil.UnsupportedChecksumAlgorithm;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.events.ConfigureEvent;
import cn.com.sparkle.firefly.event.events.ConfigureEvent.Op;
import cn.com.sparkle.firefly.net.client.syncwaitstrategy.SyncStrategyFactory;
import cn.com.sparkle.firefly.paxosinstance.paxossender.InstancePaxosMessageSenderBuilderFactory;
import cn.com.sparkle.firefly.stablestorage.util.FileUtil;

public class Configuration {
	private final static Logger logger = Logger.getLogger(Configuration.class);

	private ConfigNodeSet configNodeSet;

	private int port;
	private int clientPort;
	private String stableStorage;
	private String selfAddress = "";
	private String ip;
	private EventsManager eventsManager;
	private String filePath;
	private long responseDelay;
	private int transportTcpNum;
//	private int transportMaxInstanceRunningSize;
	private int transportSingleTcpMaxWaitingMemSize;
	private String netLayer;
	private int diskMemLost;
	private boolean debugLog;
	private int catchUpDelay;
	private boolean electSelfMaster;
	private int voteValueSplitSize;
	private int netChecksumType;
	private int fileChecksumType;
	private String sessionSuccessSyncMaxMemStrategy;
	private long sessionSuccessSyncMaxMem;
	private long sessionSuccessSyncMaxMemWaitTime;
	private String paxosSender;
	private int heartBeatInterval;
	private int electionPriority;
	private long transportTimeout;
	private String fileOutFacotryClass;
	private int maxInstanceMergePackageSize;
	private int minInstanceMergePackageSize;
	
	
	
	private boolean isMergeClientRequest;
	private String room;
	
	private HashMap<String,String> allConfig = new HashMap<String, String>();
	
	public Configuration(String filePath, EventsManager eventsManager) throws ParserConfigurationException, SAXException, IOException {
		this.eventsManager = eventsManager;
		this.filePath = filePath;

		checkNewFile();

		Properties senatorProp = new Properties();
		File senator = new File(filePath + "/senators.prop");
		senatorProp.load(new InputStreamReader(new FileInputStream(senator)));
		long version = Long.parseLong(senatorProp.getProperty("version"));
//		HashMap<String, HashSet<String>> roomDisributedMap = new HashMap<String, HashSet<String>>();
		HashSet<ConfigNode> senators = new HashSet<ConfigNode>();
		HashMap<String, ConfigNode> senatorsMap = new HashMap<String, ConfigNode>();
		for (Entry<Object, Object> e : senatorProp.entrySet()) {
			String key = (String) e.getKey();
			String value = (String) e.getValue();
			if (key.indexOf("senators") > -1) {
//				HashSet<String> sameRoomNodes = new HashSet<String>();
//				String room = key.split("_")[1];
//				roomDisributedMap.put(room, sameRoomNodes);
				for (String address : value.split(",")) {
					if (address.trim().length() != 0) {
						address = address.trim();
						ConfigNode configNode = ConfigNode.parseNode(address);
						senators.add(configNode);
						senatorsMap.put(configNode.getAddress(), configNode);
					}
				}
			}
		}
		Properties roomProp = new Properties();
		File roomPropFile = new File(filePath + "/room.prop");
		roomProp.load(new InputStreamReader(new FileInputStream(roomPropFile)));
		if (roomProp.getProperty("room") == null) {
			throw new ConfigurationError("room not be set, please check your configuration!");
		} else {
			this.room = roomProp.getProperty("room").trim().toLowerCase();
		}
		
		
		this.configNodeSet = new ConfigNodeSet(senators, senatorsMap, version);

		if (senators.size() == 0) {
			throw new ConfigurationError("one senator at least are needed!");
		}
		//		nodes.addAll(followers);

		Properties confProp = new Properties();
		File conf = new File(filePath + "/config.prop");
		confProp.load(new InputStreamReader(new FileInputStream(conf)));

		parseConfiguration(confProp);

		File file = FileUtil.getDir(this.stableStorage);
		if (file.exists() && !file.isDirectory()) {
			throw new ConfigurationError("stable-storage is not directory!");
		}
		// format file path
		this.stableStorage = file.getCanonicalPath();
		
		for(Entry<Object, Object> e : confProp.entrySet()){
			allConfig.put((String)e.getKey(), (String)e.getValue());
		}
	}

	private void parseConfiguration(Properties confProp) {
		if (confProp.getProperty("stable-storage") == null) {
			throw new ConfigurationError("stable-storage not be set, please check your configuration!");
		} else {
			this.stableStorage = confProp.getProperty("stable-storage");
		}
		
		
		if (confProp.getProperty("ip") == null) {
			throw new ConfigurationError("ip not be set, please check your configuration!");
		} else {
			this.ip = confProp.getProperty("ip");
		}
		if (confProp.getProperty("listen-port") == null) {
			throw new ConfigurationError("listen-port not be set, please check your configuration!");
		} else {
			try {
				this.port = Integer.parseInt(confProp.getProperty("listen-port").trim());
				this.selfAddress = ip + ":" + port;
			} catch (NumberFormatException e) {
				throw new ConfigurationError("listen-port must be number, please check your configuration!");
			}
		}
		
		if (confProp.getProperty("listen-port-client") == null) {
			throw new ConfigurationError("listen-port-client not be set, please check your configuration!");
		} else {
			try {
				this.clientPort = Integer.parseInt(confProp.getProperty("listen-port-client").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("listen-port-client must be number, please check your configuration!");
			}

		}
		if (confProp.getProperty("transport-tcp-num") == null) {
			throw new ConfigurationError("transport-tcp-num not be set, please check your configuration!");
		} else {
			try {
				this.transportTcpNum = Integer.parseInt(confProp.getProperty("transport-tcp-num").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("transport-tcp-num must be number, please check your configuration!");
			}
		}
		
		if (confProp.getProperty("transport-singletcp-max-waiting-mem-size") == null) {
			throw new ConfigurationError("transport-singletcp-max-waiting-mem-size not be set, please check your configuration!");
		} else {
			try {
				this.transportSingleTcpMaxWaitingMemSize = Integer.parseInt(confProp.getProperty("transport-singletcp-max-waiting-mem-size").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("transport-singletcp-max-waiting-mem-size must be number, please check your configuration!");
			}
		}
		
//		if (confProp.getProperty("transport-max-wait-queue-size") == null) {
//			throw new ConfigurationError("transport-max-wait-queue-size not be set, please check your configuration!");
//		} else {
//			try {
//				this.transportMaxWaitQueueSize = Integer.parseInt(confProp.getProperty("transport-max-wait-queue-size").trim());
//			} catch (NumberFormatException e) {
//				throw new ConfigurationError("transport-max-wait-queue-size must be number, please check your configuration!");
//			}
//		}

		if (confProp.getProperty("net-layer") == null) {
			throw new ConfigurationError("net-layer not be set, please check your configuration!");
		} else {
			this.netLayer = confProp.getProperty("net-layer").trim().toLowerCase();
			if (!this.netLayer.equals("netty") && !this.netLayer.equals("raptor")) {
				throw new ConfigurationError("net-layer must be netty or raptor!");
			}
		}
		if (confProp.getProperty("max-response-delay") == null) {
			throw new ConfigurationError("max-response-delay not be set, please check your configuration!");
		} else {
			try {
				this.responseDelay = Long.parseLong(confProp.getProperty("max-response-delay").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("max-response-delay must be number, please check your configuration!");
			}

		}
		if (confProp.getProperty("disk-mem-lost") == null) {
			throw new ConfigurationError("disk-mem-lost not be set, please check your configuration!");
		} else {
			try {
				this.diskMemLost = Integer.parseInt(confProp.getProperty("disk-mem-lost").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("disk-mem-lost must be number, please check your configuration!");
			}

		}
		if (confProp.getProperty("debug-log") == null) {
			throw new ConfigurationError("debug-log not be set, please check your configuration!");
		} else {
			String temp = confProp.getProperty("debug-log");
			if (!temp.trim().equals("true") && !temp.trim().equals("false")) {
				throw new ConfigurationError("debug-log must be true or false, please check your configuration!");
			}
			this.debugLog = Boolean.parseBoolean(temp);
		}
		if (confProp.getProperty("elect-self-master") == null) {
			throw new ConfigurationError("elect-self-master not be set, please check your configuration!");
		} else {
			String temp = confProp.getProperty("elect-self-master");
			if (!temp.trim().equals("true") && !temp.trim().equals("false")) {
				throw new ConfigurationError("elect-self-master must be true or false, please check your configuration!");
			}
			this.electSelfMaster = Boolean.parseBoolean(temp);
		}
		if (confProp.getProperty("catchup-delay") == null) {
			throw new ConfigurationError("catchup-delay not be set, please check your configuration!");
		} else {
			try {
				this.catchUpDelay = Integer.parseInt(confProp.getProperty("catchup-delay").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("catchup-delay must be number, please check your configuration!");
			}

		}
		
		if (confProp.getProperty("max-instance-merge-package-size") == null) {
			throw new ConfigurationError("max-instance-merge-package-size not be set, please check your configuration!");
		} else {
			try {
				this.maxInstanceMergePackageSize = Integer.parseInt(confProp.getProperty("max-instance-merge-package-size").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("max-instance-merge-package-size must be number, please check your configuration!");
			}

		}
		if (confProp.getProperty("min-instance-merge-package-size") == null) {
			throw new ConfigurationError("min-instance-merge-package-size not be set, please check your configuration!");
		} else {
			try {
				this.minInstanceMergePackageSize = Integer.parseInt(confProp.getProperty("min-instance-merge-package-size").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("min-instance-merge-package-size must be number, please check your configuration!");
			}

		}
		
		if (confProp.getProperty("vote-value-split-size") == null) {
			throw new ConfigurationError("vote-value-split-size not be set, please check your configuration!");
		} else {
			try {
				this.voteValueSplitSize = Integer.parseInt(confProp.getProperty("vote-value-split-size").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("vote-value-split-size must be number, please check your configuration!");
			}

		}

		if (confProp.getProperty("net-checksum-type") == null) {
			throw new ConfigurationError("net-checksum-type not be set, please check your configuration!");
		} else {
			try {
				this.netChecksumType = Integer.parseInt(confProp.getProperty("net-checksum-type").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("net-checksum-type must be number, please check your configuration");
			}
			//check if the algorithm exist.
			try {
				ChecksumUtil.getCheckSumAlgorithm(this.netChecksumType);
			} catch (UnsupportedChecksumAlgorithm e) {
				throw new ConfigurationError("net checksum type is not supported!", e);
			}
		}

		if (confProp.getProperty("file-checksum-type") == null) {
			throw new ConfigurationError("file-checksum-type not be set, please check your configuration!");
		} else {
			try {
				this.fileChecksumType = Integer.parseInt(confProp.getProperty("file-checksum-type").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("file-checksum-type must be number, please check your configuration");
			}
			//check if the algorithm exist.
			try {
				ChecksumUtil.getCheckSumAlgorithm(this.fileChecksumType);
			} catch (UnsupportedChecksumAlgorithm e) {
				throw new ConfigurationError("file checksum type is not supported!", e);
			}
		}

		if (confProp.getProperty("session-success-sync-max-mem-strategy") == null) {
			throw new ConfigurationError("session-success-sync-max-mem-strategy not be set, please check your configuration!");
		} else {
			this.sessionSuccessSyncMaxMemStrategy = confProp.getProperty("session-success-sync-max-mem-strategy").trim();
			try {
				SyncStrategyFactory.build(this);//check if the strategy is existed
			} catch (RuntimeException e) {
				throw new ConfigurationError(e.getMessage());
			}
		}

		if (confProp.getProperty("session-success-sync-max-mem") == null) {
			throw new ConfigurationError("session-success-sync-max-mem not be set, please check your configuration!");
		} else {
			try {
				this.sessionSuccessSyncMaxMem = Long.parseLong(confProp.getProperty("session-success-sync-max-mem").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("session-success-sync-max-mem must be number, please check your configuration");
			}
		}

		if (confProp.getProperty("session-success-sync-max-mem-waittime") == null) {
			throw new ConfigurationError("session-success-sync-max-mem-waittime not be set, please check your configuration!");
		} else {
			try {
				this.sessionSuccessSyncMaxMemWaitTime = Long.parseLong(confProp.getProperty("session-success-sync-max-mem-waittime").trim());
			} catch (NumberFormatException e) {
				throw new ConfigurationError("session-success-sync-max-mem-waittime must be number, please check your configuration");
			}
		}
		if (confProp.getProperty("paxos-sender") == null) {
			throw new ConfigurationError("paxos-sender not be set, please check your configuration!");
		} else {
			this.paxosSender = confProp.getProperty("paxos-sender").trim();
			try {
				InstancePaxosMessageSenderBuilderFactory.getBuilder(this);//check if the sender is existed
			} catch (RuntimeException e) {
				throw new ConfigurationError(e.getMessage());
			}
		}
		if (confProp.getProperty("heart-beat-interval") == null) {
			throw new ConfigurationError("heart-beat-interval not be set, please check your configuration!");
		} else {
			try {
				this.heartBeatInterval = Integer.parseInt(confProp.getProperty("heart-beat-interval").trim());
				if (this.heartBeatInterval > Constants.MAX_HEART_BEAT_INTERVAL) {
					this.heartBeatInterval = Constants.MAX_HEART_BEAT_INTERVAL;
					logger.info("The heart beat max interval limit is :" + Constants.MAX_HEART_BEAT_INTERVAL);
				}
			} catch (NumberFormatException e) {
				throw new ConfigurationError("heart-beat-interval must be number, please check your configuration!");
			}
		}
		if (confProp.getProperty("election-priority") == null) {
			throw new ConfigurationError("election-priority not be set, please check your configuration!");
		} else {
			try {
				this.electionPriority = Integer.parseInt(confProp.getProperty("election-priority").trim());
				if (this.electionPriority < 1 || this.electionPriority > 10) {
					throw new ConfigurationError("valid range of election-priority is [1 to 10]");
				}
			} catch (NumberFormatException e) {
				throw new ConfigurationError("election-priority must be number, please check your configuration!");
			}
		}
		if (confProp.getProperty("transport-timeout") == null) {
			throw new ConfigurationError("transport-timeout  not be set, please check your configuration!");
		} else {
			try {
				this.transportTimeout = Long.parseLong(confProp.getProperty("transport-timeout"));
			} catch (NumberFormatException e) {
				throw new ConfigurationError("transport-timeout must be number, please check your configuration!");
			}
		}
		
		
		if (confProp.getProperty("is-merge-client-request") == null) {
			throw new ConfigurationError("debug-log not be set, please check your configuration!");
		} else {
			String temp = confProp.getProperty("is-merge-client-request");
			if (!temp.trim().equals("true") && !temp.trim().equals("false")) {
				throw new ConfigurationError("is-merge-client-request must be true or false, please check your configuration!");
			}
			this.isMergeClientRequest = Boolean.parseBoolean(temp);
		}
		if (confProp.getProperty("fileOutFacotry-class") == null) {
			throw new ConfigurationError("fileOutFacotry-class not be set, please check your configuration!");
		} else {
			this.fileOutFacotryClass = confProp.getProperty("fileOutFacotry-class").trim();
		}
		
		
		logger.info("system configuration");
		logger.info("catch-up-delay:" + this.catchUpDelay);
		logger.info("service-port:" + this.clientPort);
		logger.info("debug-log:" + this.debugLog);
		logger.info("disk-mem-lost:" + this.diskMemLost);
		logger.info("configuration-file-path:" + this.filePath);
		logger.info("net-layer:" + this.netLayer);
		logger.info("room:" + this.room);
		logger.info("ip:" + this.ip);
		logger.info("system-port:" + this.port);
		logger.info("response-delay:" + this.responseDelay);
		logger.info("stable-storage-path:" + this.stableStorage);
		logger.info("transport-tcp-num:" + this.transportTcpNum);
		logger.info("transport-singletcp-max-waiting-mem-size:" + this.transportSingleTcpMaxWaitingMemSize);
		logger.info("elect-self-master:" + this.electSelfMaster);
		logger.info("vote-value-split-size:" + this.voteValueSplitSize);
		logger.info("max-instance-merge-package-size:" + this.maxInstanceMergePackageSize);
		logger.info("net-checksum-type:" + this.netChecksumType);
		logger.info("file-checksum-type:" + this.fileChecksumType);
		logger.info("session-success-sync-max-mem-strategy:" + this.sessionSuccessSyncMaxMemStrategy);
		logger.info("session-success-sync-max-mem:" + this.sessionSuccessSyncMaxMem);
		logger.info("session-success-sync-max-mem-waittime:" + this.sessionSuccessSyncMaxMemWaitTime);
		logger.info("paxos-sender:" + this.paxosSender);
		logger.info("heart-beat-interval:" + this.heartBeatInterval);
		logger.info("election-priority:" + this.electionPriority);
		logger.info("transport-timeout:" + this.transportTimeout);
		logger.info("fileOutFacotry-class:" + this.fileOutFacotryClass );
		logger.info("is-merge-client-request:" + this.isMergeClientRequest);
	}

	public int getMinInstanceMergePackageSize() {
		return minInstanceMergePackageSize;
	}

	

	public int getTransportSingleTcpMaxWaitingMemSize() {
		return transportSingleTcpMaxWaitingMemSize;
	}

	public String getRoom() {
		return room;
	}

	public boolean isMergeClientRequest() {
		return isMergeClientRequest;
	}

	public synchronized void addSenator(String address, long version) throws ConfigurationException {
		if(isDebugLog()){
			logger.debug(String.format("add %s ,version:%s targetVersion: %s",address,this.configNodeSet.getVersion(),version));
		}
		if (version <= this.configNodeSet.getVersion()) {
			return;
		}
		ConfigNodeSet newSet = (ConfigNodeSet) configNodeSet.clone();

//		HashSet<String> sameRoomNode = newSet.getRoomDistributedMap().get(roomName);
//		if (sameRoomNode == null) {
//			sameRoomNode = new HashSet<String>();
//			newSet.getRoomDistributedMap().put(roomName, sameRoomNode);
//		}
		ConfigNode senator = ConfigNode.parseNode(address);
		newSet.getSenators().add(senator);
		newSet.getSenatorsMap().put(senator.getAddress(), senator);
		newSet.setVersion(version);
		this.configNodeSet = newSet;
		
		saveSenator();
		ConfigureEvent.doSenatorsChangeEvent(eventsManager, newSet.getSenators(),senator,Op.ADD, version);
	}
	
	public synchronized void removeSenator(String address, long version) throws ConfigurationException {
		if(isDebugLog()){
			logger.debug(String.format("remove %s ,version:%s targetVersion: %s",address,this.configNodeSet.getVersion(),version));
		}
		if (version <= this.configNodeSet.getVersion()) {
			return;
		}
		ConfigNodeSet newSet = (ConfigNodeSet) configNodeSet.clone();
		ConfigNode oldsenator = configNodeSet.getSenatorsMap().get(address);
		ConfigNode senator = newSet.getSenatorsMap().get(address);
		if (senator != null && newSet.getSenators().size() == 1) {
			throw new ConfigurationException("The system must need one senator at least!");
		} else if (senator != null) {
//			senator.getSameRoomNode().remove(senator.getAddress());
//			if (senator.getSameRoomNode().size() == 0) {
//				newSet.getRoomDistributedMap().remove(senator.getRoom());
//			}
			newSet.getSenatorsMap().remove(senator.getAddress());
			newSet.getSenators().remove(senator);
//			newSet.getRoomDistributedMap().remove(senator.getAddress());
			newSet.setVersion(version);
			this.configNodeSet = newSet;
			saveSenator();
			ConfigureEvent.doSenatorsChangeEvent(eventsManager, newSet.getSenators(),oldsenator,Op.REMOVE, version);
		}
	}
	
	public String getIp(){
		return ip;
	}
	
	private void checkNewFile() {
		File sf = new File(filePath + "/senators.prop");
		File sfn = new File(filePath + "/senators.prop.new");
		if (!sf.exists()) {
			if (!sfn.exists()) {
				throw new ConfigurationError("old and new senator configuration is not existed!");
			} else {
				FileUtil.rename(sfn, sf);
			}
		}
		FileUtil.deleteFile(sfn);
		
		
		sf = new File(filePath + "/room.prop");
		sfn = new File(filePath + "/room.prop.new");
		if (!sf.exists()) {
			if (!sfn.exists()) {
				throw new ConfigurationError("old and new room configuration is not existed!");
			} else {
				FileUtil.rename(sfn, sf);
			}
		}
		FileUtil.deleteFile(sfn);
		
	}

	public boolean isElectSelfMaster() {
		return electSelfMaster;
	}

	public int getCatchUpDelay() {
		return catchUpDelay;
	}
	
	
	public int getVoteValueSplitSize() {
		return voteValueSplitSize;
	}

	public int getMaxInstanceMergePackageSize() {
		return maxInstanceMergePackageSize;
	}

	public long getTransportTimeout() {
		return transportTimeout;
	}
	
	public synchronized void changeRoom(String room){
		
		Properties p = new Properties();
		p.setProperty("room", room);
		File f;
		try {
			f = FileUtil.getFile(filePath + "/room.prop.new");
		} catch (IOException e2) {
			throw new ConfigurationError("room configuration write file failed.");
		}
		OutputStream os = null;
		try {
			os = new FileOutputStream(f);
			p.store(os, "");
			os.close();
			File ff = new File(filePath + "/room.prop");
			FileUtil.deleteFile(ff);
			FileUtil.rename(f, ff);
			this.room = room;
		} catch (IOException e) {
			throw new ConfigurationError("room configuration write file failed.");
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Throwable e) {
				}
			}
		}
	}

	private void saveSenator() {
		Properties p = new Properties();
		p.setProperty("version", String.valueOf(this.configNodeSet.getVersion()));

		StringBuffer sb = new StringBuffer();
		for (ConfigNode senator : this.configNodeSet.getSenators()) {
			sb.append(senator.getAddress()).append(",");
		}

//		for (Entry<String, StringBuffer> e : map.entrySet()) {
//			p.setProperty("senators_" + e.getKey(), e.getValue().toString());
//		}
		p.setProperty("senators", sb.toString());
		
			File f;
			try {
				f = FileUtil.getFile(filePath + "/senators.prop.new");
			} catch (IOException e1) {
				throw new ConfigurationError("senator configuration write file failed.");
			}
		OutputStream os = null;
		try {
			os = new FileOutputStream(f);
			p.store(os, "");
			os.close();
			File ff = new File(filePath + "/senators.prop");
			FileUtil.deleteFile(ff);
			FileUtil.rename(f, ff);
		} catch (IOException e) {
			throw new ConfigurationError("senator configuration write file failed.");
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Throwable e) {
				}
			}
		}
	}

	public int getTransportTcpNum() {
		return transportTcpNum;
	}

	public int getClientPort() {
		return clientPort;
	}

	public long getResponseDelay() {
		return responseDelay;
	}

	public String getStableStorage() {
		return stableStorage;
	}

	public String getSelfAddress() {
		return selfAddress;
	}
	
	public void setStableStorage(String stableStorage) {
		this.stableStorage = stableStorage;
	}

	public int getDiskMemLost() {
		return diskMemLost;
	}

	public ConfigNodeSet getConfigNodeSet() {
		return configNodeSet;
	}

	public int getPort() {
		return port;
	}

	public String getNetLayer() {
		return netLayer;
	}

	public String getFilePath() {
		return filePath;
	}

	public boolean isDebugLog() {
		return debugLog;
	}

	public int getNetChecksumType() {
		return netChecksumType;
	}

	public int getFileChecksumType() {
		return fileChecksumType;
	}

	public static Logger getLogger() {
		return logger;
	}

	public String getSessionSuccessSyncMaxMemStrategy() {
		return sessionSuccessSyncMaxMemStrategy;
	}

	public long getSessionSuccessSyncMaxMem() {
		return sessionSuccessSyncMaxMem;
	}

	public long getSessionSuccessSyncMaxMemWaitTime() {
		return sessionSuccessSyncMaxMemWaitTime;
	}

	public String getPaxosSender() {
		return paxosSender;
	}

	public int getHeartBeatInterval() {
		return heartBeatInterval;
	}

	public int getElectionPriority() {
		return electionPriority;
	}
	
	
	
	public String getFileOutFacotryClass() {
		return fileOutFacotryClass;
	}

	public String getConfigValue(String key){
		return allConfig.get(key);
	}
	
}
