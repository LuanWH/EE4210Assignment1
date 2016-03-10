package syncpeer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.Vector;

class ClientProcess extends SyncProcess {

	public static final String CLIENT_PROCESS_NAME = "ClientProcess";

	private Socket socket;

	ClientProcess(File folder, int port, String ipAddr) {
		this.name = CLIENT_PROCESS_NAME;
		this.folder = folder;
		this.port = port;
		this.ipAddr = ipAddr;
		this.isClosed = false;
	}

	/**
	 * Set the server IP for connection.
	 * @param ipAddr The server IP address
	 */
	public void setServerIp(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	
	/**
	 * Synchronize the file name list with server. It sends client local
	 * file name lists and receive lists from server indicating the client
	 * missing files and server missing files.
	 * @param fileList The set of local file name list.
	 * @return Vector of client missing file list and server missing file list
	 */
	private Vector<Set<String>> syncFileList(Set<String> fileList){
		try{
			//Send sync request to server
			boolean success = false;
			success = sendMsg(TYPE_SYNC, NIL, NIL);
			if(!success)return null;

			//Send the local file list to server
			success = sendFileList(fileList);
			if(!success)return null;

			//Receive the missing file lists from server
			Vector<Set<String>> result = new Vector<Set<String>>();
			for(int i = 0; i < FILE_LIST_LENGTH; ++i){
				Set<String> recv = receiveFileList();
				if(recv == null){
					return null;
				} else {
					result.add(recv);
				}
			}
			return result;
		} catch (IOException | ClassNotFoundException e){
			System.out.println(name + ": " + e.getMessage());
			return null;
		}
	}

	/**
	 * Request a missing file from server.
	 * @param fileName The client missing file name.
	 * @return A boolean value telling whether the request is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
	@SuppressWarnings("unchecked")
	private boolean requestFile(String fileName) {
		try {
			//Send the file request to server.
			boolean success = false;
			success = sendMsg(TYPE_REQUEST, fileName, NIL);
			if(!success) return false;

			//Receive the file from server.
			Vector<String> fileInfo = (Vector<String>) ois.readObject();
			return receiveFile(fileInfo);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Start the client and connect to the server for synchronization.
	 * It asks the server for the differences in file lists and request
	 * client missing files from the server followed by pushing server 
	 * missing files to the server.
	 */
	@Override
	public void run() {
		try {
			if (ipAddr == null) {
				System.out.println("ipAddr not set!");
				return;
			}
			
			System.out.println(name+": Establishing connection to "+this.ipAddr);

			socket = new Socket(ipAddr, port);
			System.out.println(name+": Connection established.");
			
			oos = new ObjectOutputStream(
					socket.getOutputStream());
			ois = new ObjectInputStream(
					socket.getInputStream());

			
			Set<File> fileList = getFileList();
			Set<String> fileNameList = getFileNameList(fileList);

			Vector<Set<String>> vLists = syncFileList(fileNameList);

			if(vLists == null){
				System.out.println("Unable to sync file lists.");
			} else {
				Set<String> missingFileNameList = vLists.get(MISSING_FILE_LIST_INDEX);
				Set<String> extraFileNameList = vLists.get(EXTRA_FILE_LIST_INDEX);
				for (String s : missingFileNameList) {
					System.out.println(name+": request file "+s);
					requestFile(s);
				}
				for (String s : extraFileNameList) {
					System.out.println(name+": push file "+s);
					pushFile(s);
				}
			}

			ois.close();
			oos.close();
			socket.close();
			
			System.out.println(name+": synchronization finished!");
		}catch(UnknownHostException|SocketException e){
			System.out.println(name + ": Failed to establish connection to "+this.ipAddr+". ");
			System.out.println("Reason: "+e.getMessage());
		} catch (Exception e) {
			System.out.println(name + ": Something went wrong - "+e.getMessage()+". Please try again.");
		}
	}
}