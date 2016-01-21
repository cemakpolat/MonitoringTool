package conn;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;



public class JavaToJavaClient {

	public String className = "Client: "; 

	private static String serverIpAddress;
	private boolean connected = false;
	private int  portAddress; 

	/**
	 * Creates a Client for the specified context.
	 * 
	 * @param cont
	 *            The Context of the Client.
	 */
	public JavaToJavaClient(int PORT) {
		portAddress = PORT; 
	}

	/**
	 * Sends a string to the server with a Pop-up to ask the IP address of the
	 * server.
	 * 
	 * @param tosend
	 *            The String to be sent.
	 */
	public long send(final String tosend, final String serverIP) {
		JavaToJavaClient.serverIpAddress = serverIP;
		return serverContact(tosend);
	}


	/**
	 * Sends a String to the server.
	 * 
	 * @param tosend
	 *            The String to be sent.
	 */
	private long serverContact(String tosend) {
		try {
			InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
			Socket socket = new Socket(serverAddr, portAddress ); 
			socket.setSoTimeout(5000);
			long after = 0, before = 0;
			before = System.currentTimeMillis();
			PrintWriter out = new PrintWriter(new BufferedWriter(
					new OutputStreamWriter(socket.getOutputStream())), true);
			out.println(tosend);
			after = System.currentTimeMillis();
			socket.close();
			System.out.println(className + "client QoS values in RTT (round trip time): " + String.valueOf(after - before));
			return (after - before); 
		} catch (SocketTimeoutException s) {
			return 1; 
		} catch (ConnectException e){
			System.out.println(className + "ConnectException error " + e.getMessage());
			return 2; 
		}
		catch (Exception e) {
			System.out.println(className + "Connection error " + e.getMessage());
			return -1; 
		}
	}

}
