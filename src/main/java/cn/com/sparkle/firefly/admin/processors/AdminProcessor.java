package cn.com.sparkle.firefly.admin.processors;

import cn.com.sparkle.firefly.admin.AdminLookupHandler;
import cn.com.sparkle.firefly.model.AddRequest;
import cn.com.sparkle.firefly.net.netlayer.PaxosSession;

public interface AdminProcessor {
	public void process(String[] command,AdminLookupHandler handler,PaxosSession session,AddRequest request);
	public abstract int commandLength();
	public abstract String processComm(String[] command,AdminLookupHandler handler,PaxosSession session,AddRequest request);
	public String[] getName();
}
