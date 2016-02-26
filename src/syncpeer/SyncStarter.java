package syncpeer;

import java.io.*;

public class SyncStarter {

	public static final int PORT = 14210;

	public static void main(String[] args) {
	
		if(args.length == 0){
			System.out.println("Welcome to SyncPeer!"+
							   "Usage: \n"+
					           "    Start the peer by specifying the folder to be synced\n"+
							   "      and/or the IP address of the other peer.\n"+
							   "    java SyncStarter <folder name> [<ip addr>]");
			return;
		}
		
		String _folderPath = args[0].trim();
		File folder = new File(_folderPath);
		if(!(folder.exists() && folder.isDirectory())){
			boolean suc = folder.mkdir();
			if(!suc){
				System.out.println("Unable to create directory "+_folderPath+"!");
				return;
			}
		}
		if(!(folder.exists() &&folder.isDirectory())){
			System.out.println("Unable to locate directory "+_folderPath+"_folderPath!");
			return;
		}
		
		String ipAddr = null;
		if(args.length >= 2){
			ipAddr = args[1];
		}

		Peer peer = new Peer(folder, PORT, ipAddr);

		try{
			peer.start();
		} catch(Exception e){
			e.printStackTrace();
		}
		
		System.out.println("Sync Starter terminated.");
	}
}
