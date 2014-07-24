package cn.com.sparkle.paxos;

import cn.com.sparkle.paxos.addprocess.AddRequestDealer;
import cn.com.sparkle.paxos.config.Configuration;
import cn.com.sparkle.paxos.deamon.InstanceExecutor;
import cn.com.sparkle.paxos.event.EventsManager;
import cn.com.sparkle.paxos.protocolprocessor.ProtocolManager;
import cn.com.sparkle.paxos.stablestorage.AccountBook;
import cn.com.sparkle.paxos.state.ClusterState;

public class Context {
	private Configuration configuration;
	private ProtocolManager protocolManager;
	private ClusterState cState;
	private EventsManager eventsManager;
	private AccountBook accountBook;
	private AddRequestDealer addRequestDealer;
	private InstanceExecutor instanceExecutor;

	public Context(Configuration configuration, ClusterState cState, EventsManager eventsManager) {
		super();
		this.configuration = configuration;
		this.cState = cState;
		this.eventsManager = eventsManager;
	}
	
	public void setAddRequestDealer(AddRequestDealer addRequestDealer) {
		this.addRequestDealer = addRequestDealer;
	}
	

	public InstanceExecutor getInstanceExecutor() {
		return instanceExecutor;
	}

	public void setInstanceExecutor(InstanceExecutor instanceExecutor) {
		this.instanceExecutor = instanceExecutor;
	}

	public AddRequestDealer getAddRequestDealer() {
		return addRequestDealer;
	}

	public AccountBook getAccountBook() {
		return accountBook;
	}

	public void setAccountBook(AccountBook accountBook) {
		this.accountBook = accountBook;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public void setProtocolManager(ProtocolManager protocolManager) {
		this.protocolManager = protocolManager;
	}

	public void setcState(ClusterState cState) {
		this.cState = cState;
	}

	public void setEventsManager(EventsManager eventsManager) {
		this.eventsManager = eventsManager;
	}

	public ProtocolManager getProtocolManager() {
		return protocolManager;
	}

	public ClusterState getcState() {
		return cState;
	}

	public EventsManager getEventsManager() {
		return eventsManager;
	}

}
