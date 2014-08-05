package cn.com.sparkle.firefly.client;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;

import cn.com.sparkle.firefly.client.deamon.MasterHeartBeatDeamon;
import cn.com.sparkle.firefly.deamon.ReConnectDeamon;
import cn.com.sparkle.firefly.net.client.NetNode;
import cn.com.sparkle.firefly.net.client.user.ConnectConfig;
import cn.com.sparkle.firefly.net.client.user.ConnectConfig.ConnectEvent;
import cn.com.sparkle.firefly.net.client.user.UserClientHandler;
import cn.com.sparkle.firefly.net.client.user.UserNetNode;
import cn.com.sparkle.firefly.net.client.user.callback.AddRequestCallBack;
import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetCloseException;
import cn.com.sparkle.firefly.net.netlayer.NetFactory;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;

public class CommandAsyncProcessor implements Runnable {
	private final static Logger logger = Logger.getLogger(CommandAsyncProcessor.class);
	private static NetClient nioSocketClient;
	private static UserClientHandler handler;
	private static ReConnectDeamon reConnectThread = new ReConnectDeamon();

	private String[] senator = {};

	private volatile String[] newSenator = senator;

	private ConnectConfig curConnectConfig;

	private ConnectConfig firstPriorConfig;

	private ArrayBlockingQueue<Command> commandQueue;

	private EntryNode root = new EntryNode();
	private AtomicInteger runningSize = new AtomicInteger(0);

	private final int maxRunningSize;

	private int masterDistance;

	private ReentrantLock lock = new ReentrantLock();

	private Condition wakeupCondition = lock.newCondition();

	private ReentrantLock nodeChangeLock = new ReentrantLock();

	private MasterHeartBeatDeamon heartBeatDeamon;

	private volatile boolean isWakeup = false;
	@SuppressWarnings("unused")
	private boolean debugLog;

	public static class EntryNode {
		private Command c;
		private EntryNode next;
		private EntryNode prev;

		public void setNext(EntryNode next) {
			this.next = next;
		}

		public void setPrev(EntryNode prev) {
			this.prev = prev;
		}

		public EntryNode getNext() {
			return next;
		}

		public EntryNode getPrev() {
			return prev;
		}

	}

	private ConnectEvent connectEvent = new ConnectEvent() {
		@Override
		public void disconnect(String address, NetNode node) {
			wakeup();//may be first node is disconnected
		}

		@Override
		public void connect(String address, NetNode node) {
			wakeup(); //may be first node is connected
		}
	};

	public void runDeamon() {
		Thread t = new Thread(this);
		t.setName("CommandProcessor");
		t.start();
	}

	public static void init(String confPath, String netLayerType, int preferChecksumType, int heartBeatInterval, ProtocolManager protocolManager,
			boolean debugLog) throws Throwable {
		nioSocketClient = NetFactory.makeClient(netLayerType,debugLog);
		handler = new UserClientHandler(nioSocketClient, preferChecksumType, heartBeatInterval, protocolManager, reConnectThread, debugLog);
		nioSocketClient.init(confPath, heartBeatInterval, handler,"userclient");
		reConnectThread.startThread();

	}

	public CommandAsyncProcessor(String[] senator, int maxRunningSize, int maxWaitRunSize, int masterDistance, MasterHeartBeatDeamon heartBeatDeamon,
			boolean debugLog) throws Throwable {
		this.newSenator = senator;
		this.maxRunningSize = maxRunningSize;
		this.debugLog = debugLog;
		this.masterDistance = masterDistance;
		this.heartBeatDeamon = heartBeatDeamon;
		commandQueue = new ArrayBlockingQueue<Command>(maxWaitRunSize);
		wakeup();//notify to process first
	}

	public void changeSenator(String[] senator) throws Throwable {
		try {
			nodeChangeLock.lock();
			newSenator = senator;
			wakeup();

		} finally {
			nodeChangeLock.unlock();
		}
	}

	public void wakeup() {
		try {
			lock.lock();
			isWakeup = true;
			wakeupCondition.signal();
		} finally {
			lock.unlock();
		}
	}

	private void waitWakeup() throws InterruptedException {
		try {
			lock.lock();
			if (!isWakeup) {
				wakeupCondition.await();
			}
			isWakeup = false;
		} finally {
			lock.unlock();
		}
	}

	int testCount = 0;

	public void run() {
		try {
			while (true) {
				// reactor
				waitWakeup();
				//check new seantors array
				String[] newSenatorCache = newSenator; //avoid modify newSenator concurrently
				if (senator != newSenatorCache) {
					if (senator.length != 0 && (newSenatorCache.length == 0 || !senator[0].equals(newSenatorCache[0]))) {
						//clear context last time
						firstPriorConfig.setAutoReConnect(false);
						if (firstPriorConfig.getNode() != null) {
							firstPriorConfig.getNode().close();
						}
						firstPriorConfig = null;
					}
					if (newSenatorCache.length != 0 && firstPriorConfig == null) {
						//record context this time
						firstPriorConfig = new ConnectConfig(newSenatorCache[0], true, masterDistance, connectEvent);
						String[] tmp = newSenatorCache[0].split(":");
						boolean isSuccess = nioSocketClient.connect(tmp[0], Integer.parseInt(tmp[1]), firstPriorConfig).get();
						if (isSuccess) {
							firstPriorConfig.waitHanded(); // wait first node connect
						}
					}
					senator = newSenatorCache;
				}
				//check if first node is connected
				if (curConnectConfig != firstPriorConfig && firstPriorConfig != null && firstPriorConfig.getNode() != null
						&& !firstPriorConfig.getNode().isClose()) {
					if (curConnectConfig != null && curConnectConfig.getNode() != null) {
						curConnectConfig.getNode().close();//close connection
					}
					curConnectConfig = null;
				}
				//check curConnectConfig if is connected
				if (curConnectConfig != null && (curConnectConfig.getNode() == null || curConnectConfig.getNode().isClose())) {
					curConnectConfig = null;
				}
				if (curConnectConfig == null) {
					// reconnect master
					reConnect();
					if (curConnectConfig != null) {
						// redo
						EntryNode next = root;
						while ((next = next.next) != null) {
							try {
								sendRequestToNode(curConnectConfig, next);
							} catch (NetCloseException e) {
								logger.error("master disconnected!");
								curConnectConfig = null;
								Thread.sleep(1000);
								break;
							}
						}
					}
				}
				if (curConnectConfig != null) {
					while (commandQueue.peek() != null && runningSize.get() != maxRunningSize) {
						EntryNode e = new EntryNode();
						e.c = commandQueue.poll();
						synchronized (root) {
							e.next = root.next;
							if (root.next != null) {
								root.next.prev = e;
							}
							root.next = e;
							e.prev = root;
						}
						runningSize.incrementAndGet();
						try {
							sendRequestToNode(curConnectConfig, e);
						} catch (NetCloseException ee) {
							logger.error("master disconnect!");
							curConnectConfig = null;
							wakeup();
							break;
						}
					}
				}

			}

		} catch (Throwable e) {
			logger.error("fatal error", e);
		}
	};

	public boolean addCommand(Command c, long timeout, TimeUnit timeUnit) throws InterruptedException {
		if (timeout == 0) {
			commandQueue.put(c);
			wakeup();
			return true;
		} else if (commandQueue.offer(c, timeout, timeUnit)) {
			// logger.debug("add command");
			wakeup();
			return true;
		} else {
			return false;
		}
	}

	public UserNetNode getNode() {
		return curConnectConfig == null ? null : (UserNetNode) curConnectConfig.getNode();
	}

	private void sendRequestToNode(ConnectConfig curConnectConfig, EntryNode entryNode) throws NetCloseException {
		AddRequestCallBack callback = new AddRequestCallBack(entryNode.c, this, entryNode);
		if (entryNode.c.getValue() == null) {
			((UserNetNode) curConnectConfig.getNode()).sendHeartBeat(callback);
		} else {
			((UserNetNode) curConnectConfig.getNode()).sendAddRequest(entryNode.c.getCommandType(), entryNode.c.getInstanceId(), entryNode.c.getValue(),
					callback);
		}
	}

	private void reConnect() throws InterruptedException {
		try {
			ConnectConfig config = null;
			Thread.sleep(100); // wait 100ms to avoid broadcast storm
			if (firstPriorConfig != null && firstPriorConfig.getNode() != null && !firstPriorConfig.getNode().isClose()) {
				logger.info("connect to " + senator[0]);
				// close other senator
				config = firstPriorConfig;
			} else if (senator.length > 1) {
				for (int i = 1; i < senator.length; ++i) {
					String[] tmp = senator[i].split(":");
					ConnectConfig connectConfig = new ConnectConfig(senator[i], false, masterDistance, connectEvent);
					try {
						nioSocketClient.connect(tmp[0], Integer.parseInt(tmp[1]), connectConfig);
						if (connectConfig.waitHanded(3000)) { //wait 3 seconds
							logger.info("connect to " + senator[i]);
						} else {
							logger.info("fail to connect to " + senator[i]);
						}
					} catch (TimeoutException e) {
						logger.info("connect to " + senator[i] + " time out~");
					}

					if (connectConfig.getNode() != null && !connectConfig.getNode().isClose()) {
						config = connectConfig;
						break;
					}
				}
			}
			if (config == null) {
				Thread.sleep(2000);
				logger.error("find master position failed!");
			} else {
				curConnectConfig = config;
				heartBeatDeamon.processorChangeNode(this);
			}
		} catch (Throwable e) {
			logger.error("fatal error", e);
			System.exit(1);
		}
	}

	public EntryNode getRoot() {
		return root;
	}

	public AtomicInteger getRunningSize() {
		return runningSize;
	}

}
