package cn.com.sparkle.firefly;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import cn.com.sparkle.firefly.addprocess.AddRequestDealer;
import cn.com.sparkle.firefly.addprocess.speedcontrol.SpeedControlModel;
import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.admin.processors.AdminProcessor;
import cn.com.sparkle.firefly.config.ConfigNode;
import cn.com.sparkle.firefly.config.Configuration;
import cn.com.sparkle.firefly.deamon.CatchUpDeamon;
import cn.com.sparkle.firefly.deamon.CheckMasterSenatorStateDeamon;
import cn.com.sparkle.firefly.deamon.HeartBeatCheckDeamon;
import cn.com.sparkle.firefly.deamon.InstanceExecutor;
import cn.com.sparkle.firefly.deamon.ReConnectDeamon;
import cn.com.sparkle.firefly.event.DefaultEventManager;
import cn.com.sparkle.firefly.event.EventsManager;
import cn.com.sparkle.firefly.event.listeners.AccountBookEventListener;
import cn.com.sparkle.firefly.event.listeners.ConfigureEventListener;
import cn.com.sparkle.firefly.handlerinterface.HandlerInterface;
import cn.com.sparkle.firefly.net.client.system.SystemClientHandler;
import cn.com.sparkle.firefly.net.netlayer.NetClient;
import cn.com.sparkle.firefly.net.netlayer.NetFactory;
import cn.com.sparkle.firefly.net.netlayer.NetServer;
import cn.com.sparkle.firefly.net.systemserver.SystemServerHandler;
import cn.com.sparkle.firefly.net.userserver.UserServerHandler;
import cn.com.sparkle.firefly.protocolprocessor.ProtocolManager;
import cn.com.sparkle.firefly.stablestorage.AccountBook;
import cn.com.sparkle.firefly.state.ClusterState;

public class ClassicPaxosServer implements AccountBookEventListener, ConfigureEventListener {

	private final static Logger logger = Logger.getLogger(ClassicPaxosServer.class);
	private ReConnectDeamon reConnectThread = new ReConnectDeamon();
	private Long lastExceptInstanceId = null;
	private Context context;
	private DefaultEventManager eManager;
	private SystemServerHandler handler = null;
	private HandlerInterface userHandlerInterface = null;
	private NetClient client;

	public ClassicPaxosServer(Long lastExceptInstanceId) throws InstantiationException, IllegalAccessException {
		this.lastExceptInstanceId = lastExceptInstanceId;
		eManager = new DefaultEventManager();
		eManager.registerListener(this);
		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				logger.info("system exit~");
				LogManager.shutdown();// wait exit log
			}
		});
	}

	public ClassicPaxosServer() throws InstantiationException, IllegalAccessException {
		this(null);
	}
	
	public void init(String filePath, HandlerInterface userHandlerInterface) throws Throwable{
		init(filePath,userHandlerInterface,null);
	}
	
	public void init(String filePath, HandlerInterface userHandlerInterface, List<AdminProcessor> adminList) throws Throwable {
		this.userHandlerInterface = userHandlerInterface;

		//relocate the conf of log4j
		PropertyConfigurator.configure(filePath + "/log4j.properties");

		Configuration configuration = new Configuration(filePath, eManager);
		client = NetFactory.makeClient(configuration.getNetLayer(), configuration.isDebugLog());

		ClusterState clusterState = new ClusterState(eManager, configuration);
		this.context = new Context(configuration, clusterState, eManager);
		handler = new SystemServerHandler(getEventsManager(), configuration);
		//initiate instance executor
		InstanceExecutor ie = new InstanceExecutor(context, userHandlerInterface, lastExceptInstanceId == null ? -1 : lastExceptInstanceId);
		context.setInstanceExecutor(ie);
		// initiate account book
		AccountBook accountBook = new AccountBook(context, lastExceptInstanceId);
		context.setAccountBook(accountBook);
		userHandlerInterface.setAccountBook(accountBook);
		// initiate instance executor thread
		ie.start();

		//init admin lookup processor
		AdminLookupHandler adminHandler = new AdminLookupHandler(context, adminList == null ? new LinkedList<AdminProcessor>() : adminList);

		//init protocol manager
		ProtocolManager protocolManager = ProtocolManager.createServerProtocol(context, userHandlerInterface, adminHandler);

		// initialize server handler
		handler.initProtocolManager(protocolManager);

		// initialize speed control
		new SpeedControlModel(context);
		// initiate heart beat deamon
		HeartBeatCheckDeamon beatCheckDeamon = new HeartBeatCheckDeamon(context);
		beatCheckDeamon.start();

		// initiate catch up deamon
		Thread catchUpDeamon = new Thread(new CatchUpDeamon(context));
		catchUpDeamon.setName("catchUpDeamon");
		catchUpDeamon.start();
		reConnectThread.startThread();
		// connect all node
		connectNodes(protocolManager);
		// initiate user server
		AddRequestDealer addRequestDealer = new AddRequestDealer(context);
		userHandlerInterface.setAddRequestDealer(addRequestDealer);
		context.setAddRequestDealer(addRequestDealer);

		// init book
		accountBook.init(ie);
	}

	@Override
	public void accountInit() {
		try {
			Configuration conf = context.getConfiguration();
			ConfigNode configNode = new ConfigNode(conf.getIp(), String.valueOf(conf.getPort()));
			context.getcState().getSelfState().init(context);
			//after account book have finished initial, start to bind server and user port
			tryBind(configNode, handler);
			tryBindUserServer(userHandlerInterface);
			// initiate master check deamon
			Thread checkSenatorDeamon = new Thread(new CheckMasterSenatorStateDeamon(context));
			checkSenatorDeamon.setName("checkSenatorDeamon");
			checkSenatorDeamon.start();
		} catch (Throwable e) {
			logger.error("fatal error", e);
			System.exit(1);
		}

	}

	public EventsManager getEventsManager() {
		return eManager;
	}

	private SystemClientHandler connectNodes(ProtocolManager protocolManager) throws Throwable {
		Configuration conf = context.getConfiguration();
		ClusterState cState = context.getcState();
		EventsManager eventsManager = context.getEventsManager();

		SystemClientHandler phandler = new SystemClientHandler(client, eventsManager, reConnectThread, conf, protocolManager);
		client.init(conf.getFilePath() + "/system_out_net.prop", conf.getHeartBeatInterval(), phandler, "systemclient");

		NodesCollection senators = cState.getSenators();
		for (ConfigNode node : senators.getNodeMembers()) {
			String ip = node.getIp();
			String port = node.getPort();
			client.connect(ip, Integer.parseInt(port), node);
		}
		return phandler;
	}

	private void tryBind(ConfigNode configNode, SystemServerHandler handler) throws Throwable {
		Configuration conf = context.getConfiguration();
		NetServer server = NetFactory.makeServer(conf.getNetLayer(), conf.isDebugLog());
		server.init(conf.getFilePath() + "/system_in_net.prop", conf.getHeartBeatInterval(), handler, "systemserver");

		server.listen(configNode.getIp(), Integer.parseInt(configNode.getPort()));
	}

	private UserServerHandler tryBindUserServer(HandlerInterface userHandlerInterface) throws Throwable {
		EventsManager eventsManager = context.getEventsManager();
		Configuration conf = context.getConfiguration();
		UserServerHandler handler = new UserServerHandler(eventsManager, conf, userHandlerInterface, context.getProtocolManager());
		NetServer server = NetFactory.makeServer(conf.getNetLayer(), conf.isDebugLog());
		server.init(conf.getFilePath() + "/service_in_net.prop", conf.getHeartBeatInterval(), handler, "userserver");

		server.listen(conf.getIp(), conf.getClientPort());
		return handler;
	}

	@Override
	public void senatorsChange(Set<ConfigNode> newSenators, ConfigNode addNode, ConfigNode rmNode, long version) {
		if (addNode != null) {
			String ip = addNode.getIp();
			String port = addNode.getPort();
			try {
				client.connect(ip, Integer.parseInt(port), addNode);
			} catch (NumberFormatException e) {
			} catch (Throwable e) {
				logger.error("", e);
			}
		}
	}

}
