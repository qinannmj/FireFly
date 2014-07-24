package cn.com.sparkle.raptor.test.perfomance;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class TestBlockSocketServer {
	static class DealThread extends Thread {
		private Socket s;

		public DealThread(Socket s) {
			super();
			this.s = s;
		}

		public void run() {
			try {
				InputStream is = s.getInputStream();
				OutputStream os = s.getOutputStream();
				byte[] b = new byte[128];
				while (true) {
					int size = 0;
					while (true) {
						size += is.read(b, size, b.length - size);
						if (size == b.length)
							break;
					}

					os.write(b);
					os.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * @param args
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void main(String[] args) throws IOException,
			ClassNotFoundException {

		ServerSocket ss = new ServerSocket(1234);
		while (true) {
			try {
				Socket s = ss.accept();
				s.setTcpNoDelay(true);
				new DealThread(s).start();
			} catch (Throwable e) {
				e.printStackTrace();
			}

		}
	}

}
