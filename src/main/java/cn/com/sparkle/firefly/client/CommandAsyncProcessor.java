package cn.com.sparkle.firefly.client;

import java.util.HashMap;
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
import cn.com.sparkle.firefly.net.client.user.UserClientHandler;
import cn.com.sparkle.firefly.net.client.user.UserNetNode;
import cn.com.sparkle.firefly.net.client.user.ConnectConfig.ConnectEvent;
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

	private volatile String[] senator = {};
	private volatile UserNetNode node;
	private volatile ConnectConfig firstPriorConfig;
	private volatile HashMap<String, UserNetNode> activeNode = new HashMap<String, UserNetNode>();

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
			if (node != null) {
				synchronized (this) {
					HashMap<String, UserNetNode> newSet = new HashMap<String, UserNetNode>();
					newSet.putAll(activeNode);
					newSet.remove(address);
					activeNode = newSet;
					node.close();
				}
			}
		}

		@Override
		public void connect(String address, NetNode node) {
			synchronized (this) {
				HashMap<String, UserNetNode> newSet = new HashMap<String, UserNetNode>();
				newSet.putAll(activeNode);
				newSet.put(address, (UserNetNode) node);
				activeNode = newSet;
			}
			heartBeatDeamon.processorChangeNode(CommandAsyncProcessor.this);
		}
	};

	public void runDeamon() {
		Thread t = new Thread(this);
		t.setName("CommandProcessor");
		t.start();
	}

	public static void init(String confPath, String netLayerType, int preferChecksumType, int heartBeatInterval, ProtocolManager protocolManager,
			boolean debugLog) throws Throwable {
		nioSocketClient = NetFactory.makeClient(netLayerType);
		handler = new UserClientHandler(nioSocketClient, preferChecksumType, heartBeatInterval, protocolManager, reConnectThread, debugLog);
		nioSocketClient.init(confPath, heartBeatInterval, handler);
		reConnectThread.startThread();
	}

	public CommandAsyncProcessor(String[] senator, int maxRunningSize, int maxWaitRunSize, int masterDistance, MasterHeartBeatDeamon heartBeatDeamon,
			boolean debugLog) throws Throwable {
		this.senator = senator;
		this.maxRunningSize = maxRunningSize;
		this.debugLog = debugLog;
		this.masterDistance = masterDistance;
		this.heartBeatDeamon = heartBeatDeamon;
		commandQueue = new ArrayBlockingQueue<Command>(maxWaitRunSize);

		String[] tmp = senator[0].split(":");
		firstPriorConfig = new ConnectConfig(senator[0], true, masterDistance, connectEvent);
		boolean isSuccess = nioSocketClient.connect(tmp[0], Integer.parseInt(tmp[1]), firstPriorConfig).get();
		if (isSuccess) {
			firstPriorConfig.waitHanded();
		}
	}

	public void changeSenator(String[] senator) throws Throwable {
		try {
			nodeChangeLock.lock();
			if (!senator[0].equals(this.senator[0])) {
				firstPriorConfig.setAutoReConnect(false);
				firstPriorConfig = new ConnectConfig(senator[0], true, masterDistance, connectEvent);
				String[] tmp = senator[0].split(":");
				boolean isSuccess = nioSocketClient.connect(tmp[0], Integer.parseInt(tmp[1]), firstPriorConfig).get();
				if (isSuccess) {
					firstPriorConfig.waitHanded();
				}
			}
			UserNetNode n = node;
			if (n != null && !n.getAddress().equals(senator[0])) {
				n.close();
			}
			this.senator = senator;
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

	int testCount = 0;

	public void run() {
		try {
			while (true) {
				// reactor
				try {
					lock.lock();
					if (!isWakeup) {
						wakeupCondition.await();
					}
					isWakeup = false;
				} finally {
					lock.unlock();
				}
				if (node != null && node.isClose()) {
					node = null;
				}
				while (node == null) {
					// reconnect master
					reConnect();
					// redo
					EntryNode next = root;
					while ((next = next.next) != null) {
						try {
							sendRequestToNode(node, next);
						} catch (NetCloseException e) {
							logger.error("master disconnected!");
							node = null;
							Thread.sleep(1000);
							break;
						}
					}
				}

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
						sendRequestToNode(node, e);
					} catch (NetCloseException ee) {
						logger.error("master disconnect!");
						node = null;
						wakeup();
						break;
					}
				}
				// logger.debug("runningCommand size:" + runningCommand.size());
			}

		} catch (InterruptedException e) {
			throw new RuntimeException("not support interruptedException", e);
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
		return node;
	}

	private void sendRequestToNode(UserNetNode node, EntryNode entryNode) throws NetCloseException {
		AddRequestCallBack callback = new AddRequestCallBack(entryNode.c, this, entryNode);
		if (entryNode.c.getValue() == null) {
			node.sendHeartBeat(callback);
		} else {
			node.sendAddRequest(entryNode.c.getCommandType(),entryNode.c.getInstanceId(), entryNode.c.getValue(), callback);
		}
	}

	private void reConnect() throws InterruptedException {
		try {
			UserNetNode node = null;
			Thread.sleep(100); // wait 100ms to avoid broadcast storm
			while (node == null) {
				String[] senator = this.senator;
				HashMap<String, UserNetNode> activeSenators = activeNode;
				node = activeSenators.get(senator[0]);
				if (node != null) {
					logger.info("connect to " + senator[0]);
					// close other senator
					for (java.util.Map.Entry<String, UserNetNode> e : activeSenators.entrySet()) {
						if (!e.getKey().equals(senator[0])) {
							e.getValue().close();
						}
					}
				} else {
					if (senator.length > 1) {
						for (int i = 1; i < senator.length; ++i) {
							node = activeNode.get(senator[i]);
							if (node == null) {
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
							}
							node = activeNode.get(senator[i]);
							if (node != null && !node.isClose()) {
								break;
							} else {
								node = null;
							}
						}
					}
				}

				if (node == null) {
					Thread.sleep(2000);
					logger.error("find master position failed!");
				} else {
					try {
						nodeChangeLock.lock();
						if (senator == this.senator) {
							logger.info("node is changed");
							this.node = node;
							heartBeatDeamon.processorChangeNode(this);

						} else {
							node.close();
							node = null;
						}
					} finally {
						nodeChangeLock.unlock();
					}
				}
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
