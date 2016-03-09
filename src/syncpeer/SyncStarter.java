package syncpeer;

import java.io.*;
import syncpeer.Peer;

public class SyncStarter {

	public static final int PORT = 14210;

	public static void main(String[] args) {
	
		if(args.length == 0){
			System.out.println("Welcome to SyncPeer!\n"+
							   "Usage: \n"+
					           "    Start the peer by specifying the folder to be synced\n"+
							   "      and/or the IP address of the other peer.\n"+
							   "    java SyncStarter <folder name> [<ip addr>]");
			return;
		}
		
		String _folderPath = args[0].trim();
		final File folder = new File(_folderPath);
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
		
		final String ipAddr;
		if(args.length >= 2){
			ipAddr = args[1];
		} else {
			ipAddr = null;
		}

		try{
			Thread t = new Thread(
			new Runnable(){

				@Override
				public void run() {
					Peer peer = new Peer(folder, PORT, ipAddr);
					peer.start();
				}
				
			});
			t.start();
		} catch(Exception e){
			System.out.println("Cannot start synchronization because "+e.getMessage()+". Please try again.");
		}
	}
}
