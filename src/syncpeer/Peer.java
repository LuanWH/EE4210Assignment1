package syncpeer;

import java.io.*;
import java.util.Scanner;

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
			}
			if (reading[0].equalsIgnoreCase(EXIT_COMMAND)) {
				exit();
				return;
			}
			if (reading[0].equalsIgnoreCase(CONNECT_COMMAND)) {
				connect(reading);
			}
		}
	}
	
	public void exit(){
		if(server != null){
			server.close();
		}
		if(client != null){
			client.close();
		}
		try {
			server.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("SyncPeer terminated.");
	}

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
	
	private void connect() {
		client = new ClientProcess(this.folder, this.port, this.ipAddr);
		client.start();
	}
}
