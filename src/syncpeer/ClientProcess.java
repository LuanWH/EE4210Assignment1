package syncpeer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Set;
import java.util.Vector;

class ClientProcess extends SyncProcess {

	public static final String ClientProcessName = "ClientProcess";

	protected Socket socket;

	ClientProcess(File folder, int port, String ipAddr) {
		this.name = ClientProcessName;
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
			boolean success;
			System.out.println(name + ": "+SYNC_FILE_LIST);
			oos.writeObject(SYNC_FILE_LIST);
			oos.flush();
			System.out.println(name + ": "+SYNC_FILE_LIST+ " sent.");
			String ack = (String) ois.readObject();
			if (!ack.equalsIgnoreCase(ACK_SYNC)) {
				return null;
			}
			
			success = sendFileList(fileList);
			if(!success){
				return null;
			}
			
			Vector<Set<String>> result = new Vector<Set<String>>();
			for(int i = 0; i < FILE_LIST_LENGTH; ++i){
				Set<String> recv = receiveFileList();
				if(recv == null){
					return null;
				} else {
					result.add(recv);
				}
			}
			
			System.out.println(name + ": response received!");
			
			return result;
		} catch (IOException | ClassNotFoundException e){
			System.out.println(name + ": " + e.getMessage());
			return null;
		}
	}

	private boolean pushFile(String fileName){
		try {
			oos.writeObject(PUSH_FILE);
			oos.flush();
			String ack = (String) ois.readObject();
			if (!ack.equalsIgnoreCase(ACK_PUSH)) {
				return false;
			}
			oos.writeObject(fileName);
			oos.flush();
			String nameAck = (String) ois.readObject();
			if (!nameAck.equalsIgnoreCase(ACK_NAME)) {
				return false;
			}
			return sendFile(fileName);
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}

	private boolean requestFile(String fileName) {
		try {
			System.out.println(name + ": "+REQUEST_FILE);
			oos.writeObject(REQUEST_FILE);
			oos.flush();
			System.out.println(name + ": "+REQUEST_FILE+" sent.");
			String ack = (String) ois.readObject();
			if (!ack.equalsIgnoreCase(ACK_REQUEST)) {
				return false;
			}

			oos.writeObject(fileName);
			oos.flush();
			String nameAck = (String) ois.readObject();
			if (!nameAck.equalsIgnoreCase(ACK_NAME)) {
				return false;
			}

			return receiveFile(fileName);
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
				boolean success;
				System.out.println("Client Missing files:");
				for (String s : missingFileNameList) {
					System.out.println(name+": starting request "+s);
					success = requestFile(s);
					System.out.println(name+": request "+success);
				}
				
				System.out.println();
				
				System.out.println("Client Extra files:");
				for (String s : extraFileNameList) {
					System.out.println(name+": starting push "+s);
					success = pushFile(s);
					System.out.println(name+": push "+success);
				}
			}

			ois.close();
			oos.close();
			socket.close();
			
			System.out.println(name+": synchronization finished!");
		}catch(java.net.ConnectException e){
			System.out.println(name + ": Failed to establish connection to "+this.ipAddr);
		}catch(java.net.UnknownHostException e){
			System.out.println(name + ": "+this.ipAddr+ " cannot be resolved.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}