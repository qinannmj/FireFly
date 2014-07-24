package cn.com.sparkle.paxos.admin;

import cn.com.sparkle.paxos.model.AddRequest;
import cn.com.sparkle.paxos.net.netlayer.PaxosSession;

public abstract class AbstractAdminProcessor {
	public void process(String[] command,AdminLookupHandler handler,PaxosSession session,AddRequest request){
		int commLen = commandLength();
		if(command.length < commLen){
			sendResponse("the " + commLen + " args is must needed!", handler, session, request);
		}else{
			try{
				String result = processComm(command, handler, session, request);
				sendResponse(result, handler, session, request);
			}catch(Throwable e){
				sendResponse("command has executed unsuccessfully! error info:" + e.getMessage(), handler, session, request);
			}
		}
	}
	public abstract int commandLength();
	public abstract String processComm(String[] command,AdminLookupHandler handler,PaxosSession session,AddRequest request);
	public void sendResponse(String response,AdminLookupHandler handler,PaxosSession session,AddRequest request){
		byte[] rb = response.getBytes();
		handler.sendResponseCommandResponse(session, request, rb);
	}
}
