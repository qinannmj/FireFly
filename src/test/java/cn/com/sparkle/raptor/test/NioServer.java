package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioServer implements Runnable {

	public static void main(String[] args) throws UnknownHostException,
			IOException, InterruptedException {
		for (int i = 0; i < 1; i++) {
//			Thread.sleep(2000);
			Thread t = new Thread(new NioServer());
			t.start();
		}
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		// Socket s = new Socket("122.11.54.201",1234);
		// OutputStream os = s.getOutputStream();
		// InputStream is = s.getInputStream();
		// DataOutputStream dos = new DataOutputStream(os);
		// BufferedWriter bw = new BufferedWriter( new OutputStreamWriter(os));
		// bw.write("aaaaaa");
		// bw.newLine();
		// bw.flush();
		// dos.writeUTF("aaaaaaaaaaaaaaa");
		// dos.writeUTF("bbbbbbbbbbbbbbb\r\n");
		// dos.flush();

		// bw.write("aaaa");
		// bw.newLine();
		// bw.flush();
		// while(true) {
		// byte[] buff = new byte[1024 * 1024];
		// System.out.println("[" + new Date() + "]read:" + is.read(buff));
		// }
		byte[] buff = new byte[128];
		try {
			Selector sel = Selector.open();
			for (int i = 0; i < 1; i++) {
				ServerSocketChannel server = ServerSocketChannel.open();
				server.socket().bind(new InetSocketAddress("127.0.0.1",1234));
				server.configureBlocking(false);
				server.register(sel, SelectionKey.OP_ACCEPT);
				System.out.println("fffffffff");
			}

			while (true) {
				int i = sel.select(1);
				if (i > 0) {
					Iterator iter = sel.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = (SelectionKey) iter.next();
						iter.remove();
						if(key.isAcceptable()){
							SocketChannel sc = ((ServerSocketChannel) key.channel())
									.accept();
							sc.configureBlocking(false);
							sc.socket().setTcpNoDelay(true);
							sc.register(sel, SelectionKey.OP_READ);
						} else if (key.isReadable()) {
							SocketChannel sc = (SocketChannel) key
									.channel();
							ByteBuffer bb = ByteBuffer.allocate(128);
							while(bb.hasRemaining()){
								sc.read(bb);
							}
							
							sc.write(ByteBuffer.wrap(buff));
//							System.out.println(sc.write(ByteBuffer.wrap(buff)));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
