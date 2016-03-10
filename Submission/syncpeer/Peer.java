package syncpeer;

import java.io.File;
import java.util.Scanner;

/**
 * The synchronization peer application class. It directly interacts with users and 
 * synchronizes files according to user inputs.
 * @author Wenhao
 *
 */
public class Peer {

	public static final String EXIT_COMMAND = "EXIT";
	public static final String CONNECT_COMMAND = "CONNECT";

	private int port;
	private File folder;
	private ClientProcess client;
	private ServerProcess server;
	private String ipAddr;
	private Scanner scanner;

	Peer(File folder, int port, String ipAddr) {
		this.port = port;
		this.folder = folder;
		this.ipAddr = ipAddr;
		client = null;
		server = new ServerProcess(this.folder, this.port);
		this.scanner = new Scanner(System.in);
	}

	/**
	 * Starts the peer by setting up the server thread and waiting for connections.
	 * Reading user inputs and dispatch user commands.<br>
	 * <br>
	 * The two acceptable commands are {@code CONNECT <IP Address>} and {@code EXIT}.
	 */
	public void start() {
		server.start();
		if(this.ipAddr != null &&
		   !this.ipAddr.isEmpty() && 
		   !this.ipAddr.equals("\n")){
			connect();
		}
		while (true) {
			String rawReading = scanner.nextLine();
			if (rawReading == null || rawReading.isEmpty()
					|| rawReading.equals("\n")) {
				continue;
			}
			String[] reading = rawReading.trim().split(" ");
			if (reading.length == 0 || reading[0].isEmpty()
					|| reading[0].equals("\n")) {
				continue;
			} else if (reading[0].equalsIgnoreCase(EXIT_COMMAND)) {
				exit();
				return;
			} else if (reading[0].equalsIgnoreCase(CONNECT_COMMAND)) {
				connect(reading);
			} else {
				System.out.println("Your input + "+rawReading+" cannot be handled.");
			}
		}
	}
	
	/**
	 * Close server connection and stop client synchronization if any.<br>
	 * Terminate server thread after connections are closed.
	 */
	private void exit(){
		if(server != null){
			server.close();
		}
		if(client != null){
			client.close();
		}
		try {
			server.join();
		} catch (InterruptedException e) {
			System.out.println("Something wrong when trying to safely exit.\nReason: "+e.getMessage());
		}
		System.out.println("SyncPeer terminated.");
	}

	/**
	 * Connect to a IP address and start synchronization.<br> 
	 * It calls {@link Peer#connect()} for actual execution.
	 * @param reading The user input with IP address.
	 */
	private void connect(String[] reading) {
		if (reading.length < 2 || reading[1].isEmpty() || reading[1].equals("\n")) {
			System.out.println("**CONNECT** command Usage: \n"
					+ "    CONNECT <IP Address>");
			return;
		}
		String ipAddr = reading[1];
		this.ipAddr = ipAddr;
		connect();
	}
	
	/**
	 * Internal method that executes connection command to the IP address set at ({@link Peer#ipAddr}).
	 * Call {@link Peer#connect(String[])} for setting IP address before connection.
	 */
	private void connect() {
		client = new ClientProcess(this.folder, this.port, this.ipAddr);
		client.start();
	}
}
