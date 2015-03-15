package cn.com.sparkle.raptor.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class NioClient implements Runnable {

	public static void main(String[] args) throws UnknownHostException,
			IOException, InterruptedException {
		for (int i = 0; i < 1; i++) {
//			Thread.sleep(2000);
			Thread t = new Thread(new NioClient());
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
		try {
			Selector sel = Selector.open();
			for (int i = 0; i < 1; i++) {
				SocketChannel sc = SocketChannel.open();
				sc.configureBlocking(false);
				// sc.connect(new InetSocketAddress("122.11.54.201",1234));
//				 sc.connect(new InetSocketAddress("127.0.0.1",1234));
				sc.connect(new InetSocketAddress("192.168.3.100", 1234));
				sc.socket().setSoTimeout(0);
				sc.register(sel, SelectionKey.OP_CONNECT, "" + i);
			}

			while (true) {
				int i = sel.select();
				if (i > 0) {
					Iterator iter = sel.selectedKeys().iterator();
					while (iter.hasNext()) {
						SelectionKey key = (SelectionKey) iter.next();
						iter.remove();
						if (key.isConnectable()) {
							SocketChannel thisSc = (SocketChannel) key
									.channel();
							// sc.write(ByteBuffer.wrap(new byte[1024]));
							thisSc.register(sel, SelectionKey.OP_WRITE, key
									.attachment());
							thisSc.finishConnect();
							System.out.println(key.attachment() + "connect");
						} else if (key.isReadable()) {
							SocketChannel thisSc = (SocketChannel) key
									.channel();

							ByteBuffer bb = ByteBuffer.allocate(1024);
//							 thisSc.read(bb);
//							System.out.println(key.attachment() + " recieve:"
//									+ thisSc.read(bb));
							//Thread.sleep(50);
							thisSc.write(ByteBuffer.wrap(new byte[1]));
						} else if (key.isWritable()) {
							SocketChannel thisSc = (SocketChannel) key
									.channel();
							 System.out.println(key.attachment() + "send" + thisSc.write(ByteBuffer.wrap(new byte[1024])));
							
//							thisSc.register(sel, SelectionKey.OP_READ, key
//									.attachment());
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
