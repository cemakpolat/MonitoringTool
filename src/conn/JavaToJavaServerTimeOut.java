package conn;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Timer;

import json.JSONException;
import json.JSONObject;


public class JavaToJavaServerTimeOut {

	private int port;
	private int numberClients = 0;
	private String thisLine;
	public ServerSocket serverSocket;
	
	public boolean firstLineRead = true; 
	
	public String className = "Server: "; 
	//private JTextArea textarea;

	/**
	 * Creates a Server and launching it.
	 */
	
	public String getMessageJSON(){
		String receivedMessage=""; 
		String ipOfTheClient=""; 
		try {
			serverSocket.setSoTimeout(50000);
			Socket client = serverSocket.accept();
			numberClients++;
			ipOfTheClient = client.getInetAddress().getHostAddress();
			writeConsole("IP address of this client : " + ipOfTheClient);
			BufferedReader incomingFlux = new BufferedReader(
					new InputStreamReader(client.getInputStream()));
			thisLine = incomingFlux.readLine();
			receivedMessage = thisLine + receivedMessage; 
			client.close();
		} 
		catch (SocketTimeoutException s) {
			return "timeout"; 
		}
		catch (IOException e) {
			System.out.println("Error with client number " + numberClients
					+ "\n" + e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}
		JSONObject messageObject = new JSONObject(); 
		try {
			messageObject.put("ipClient", ipOfTheClient);
			messageObject.put("receivedMessage", new JSONObject(receivedMessage)); 
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return receivedMessage; 
	}
	
	public String getMessage(){
		String receivedMessage=""; 
		String ipOfTheClient=""; 
		try {
			serverSocket.setSoTimeout(10000);
			Socket client = serverSocket.accept();
			ipOfTheClient = client.getInetAddress().getHostAddress();
			writeConsole("IP address of this client : " + ipOfTheClient);
			BufferedReader incomingFlux = new BufferedReader(
					new InputStreamReader(client.getInputStream()));
			System.out.println("Starting to read the line");
			thisLine = incomingFlux.readLine();
			receivedMessage = receivedMessage + thisLine;
			client.close();
		} 
		catch (SocketTimeoutException s) {
			return "timeout"; 
		}
		catch (IOException e) {
			System.out.println("Error with client number " + numberClients
					+ "\n" + e.getMessage());
			e.printStackTrace();
			//System.exit(1);
		}
		
		return receivedMessage; 
	}
	
	
	
	public JavaToJavaServerTimeOut(int portAddress) {
		//this.textarea = textarea;
		port = portAddress; 
		try {
			serverSocket = new ServerSocket(port);
		} catch (IOException e) {
			writeConsole("Error Message from Server :\n" + e.getMessage());
			e.printStackTrace();
			//System.exit(1);
		}
	};
	
	/**
	 * Adds a String to the JTextArea.
	 * @param towrite The String to add
	 */
	private void writeConsole(String towrite){
		System.out.println(className + towrite); 
	}

	/**
	 * Execution of the Server thread by reading the incoming flux.
	 */
}
