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

	public void setServerIp(String ipAddr) {
		this.ipAddr = ipAddr;
	}
	
	
	private Vector<Set<String>> syncFileList(Set<String> fileList){
		try{
			boolean success = false;
			success = sendMsg(TYPE_SYNC, NIL, NIL);
			if(!success)return null;

			//System.out.println("testing1");
			success = sendFileList(fileList);
			if(!success)return null;
			//System.out.println("testing2");
			Vector<Set<String>> result = new Vector<Set<String>>();
			for(int i = 0; i < FILE_LIST_LENGTH; ++i){
				//System.out.println("testing3");
				Set<String> recv = receiveFileList();
				//System.out.println("testing4");
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

	@SuppressWarnings("unchecked")
	private boolean requestFile(String fileName) {
		try {
			boolean success = false;
			success = sendMsg(TYPE_REQUEST, fileName, NIL);
			if(!success) return false;

			Vector<String> fileInfo = (Vector<String>) ois.readObject();
			return receiveFile(fileInfo);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}
	
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