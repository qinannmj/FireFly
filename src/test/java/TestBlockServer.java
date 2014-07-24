import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class TestBlockServer {
	public static void main(String[] args) throws IOException {
		ServerSocket ss = new ServerSocket(1234);
		Socket s = ss.accept();
		
	}
}
