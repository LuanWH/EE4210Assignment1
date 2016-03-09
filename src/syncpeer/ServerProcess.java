package syncpeer;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Set;
import java.util.Vector;

class ServerProcess extends SyncProcess {

	private static final String SERVER_PROCESS_NAME = "ServerProcess";

	private ServerSocket socket;

	ServerProcess(File folder, int port) {
		this.folder = folder;
		this.port = port;
		this.name = SERVER_PROCESS_NAME;
		this.isClosed = false;
	}


	private boolean requestHandler(Vector<String> request){
		try{
			String fileName = request.get(MSG_NAME_INDEX);
			if(fileName == NIL) return false;
			sendAck();

			System.out.println(name + ": send file "+fileName);
			
			return pushFile(fileName);
		} catch(IOException e){
			System.out.println(name + ": " + e.getMessage());
			return false;			
		}
	}
	
	private boolean pushHandler(Vector<String> fileInfo){
		System.out.println(name + ": receive file "+fileInfo.get(MSG_NAME_INDEX));
		return receiveFile(fileInfo);
	}
	
	private boolean syncHandler(){
		try{
			sendAck();
			Set<File> fileList = getFileList();
			Set<String> fileNameList = getFileNameList(fileList);

			Set<String> clientFileNameList = receiveFileList();
			
			if(clientFileNameList == null) {
				System.out.println("Unable to sync file lists.");
				return false;
			}

			Set<String> clientMissingFileNameList = difference(
					fileNameList, clientFileNameList);
			Set<String> clientExtraFileNameList = difference(
					clientFileNameList, fileNameList);
			
			boolean success;
			success = sendFileList(clientMissingFileNameList);
			if(!success) {
				System.out.println("Unable to sync file lists.");
				return false;
			}
			
			success = sendFileList(clientExtraFileNameList);
			if(!success) {
				System.out.println("Unable to sync file lists.");
				return false;
			}
			
			return true;
			
		} catch(IOException e){
			System.out.println(name + ": " + e.getMessage());
			return false;			
		}
	}
	
	@SuppressWarnings("unchecked")
	private Vector<String> readCommand() throws IOException{
		try{
			Vector<String> reading = (Vector<String>) ois.readObject();
			if(reading != null &&
			   reading.size() == MSG_SIZE){
				return reading;
			}
			return null;
		} catch (SocketTimeoutException e){
			return null;
		} catch (ClassNotFoundException e){
			System.out.println(name + ": "+e.getMessage());
			return null;
		}

	}
	
	private boolean dispatchCommand(Vector<String> cmd){
		String type = cmd.get(MSG_TYPE_INDEX);
		if(type == null ||
		   type.isEmpty() ||
		   type == "\n"){
			return false;
		}
		boolean success = false;
		if(type.equalsIgnoreCase(TYPE_REQUEST)){
			success = requestHandler(cmd);
		} else if(type.equalsIgnoreCase(TYPE_PUSH)){
			success = pushHandler(cmd);
		} else if(type.equalsIgnoreCase(TYPE_SYNC)){
			success = syncHandler();
		}
		return success;	
	}
	
	@Override
	public void run() {
		while (!isClosed()) {
			try {
				while (!isClosed()) {
					try {
						socket = new ServerSocket(port);
						break;
					} catch (BindException e) {
						try {
							Thread.sleep(100);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						continue;
					}
				}

				if (isClosed()) {
					if(socket != null) socket.close();
					return;
				}
				System.out.println(name + ": waiting for connection.");

				socket.setSoTimeout(TIME_OUT);

				Socket fromClientSocket = null;

				while (!isClosed()) {
					try {
						fromClientSocket = socket.accept();
						break;
					} catch (SocketTimeoutException e) {
						continue;
					}
				}

				if (isClosed()) {
					if(fromClientSocket != null) fromClientSocket.close();
					if(socket != null) socket.close();
					return;
				}
				System.out.println(name + ": connection established.");
				
				fromClientSocket.setSoTimeout(TIME_OUT*5);
				oos = new ObjectOutputStream(
						fromClientSocket.getOutputStream());
				ois = new ObjectInputStream(
						fromClientSocket.getInputStream());
				
				Vector<String> cmd;
				while(!isClosed() && !fromClientSocket.isClosed()){
					cmd = readCommand();
					if(cmd == null) {
						System.out.println(name+": Invalid command from the other peer!");
						continue;
					}
					dispatchCommand(cmd);
				}

				oos.close();
				ois.close();
				fromClientSocket.close();
				socket.close();

			} catch (SocketException | EOFException e){
				try{
					if(oos != null)oos.close();
					if(ois != null)ois.close();
					if(socket != null)socket.close();
				} catch (Exception e1){
					System.out.println(name+": Something went wrong - "+e1.getMessage());
				}
				System.out.println(name+": connected was closed by the peer.");
			} catch (IOException e) {
				System.out.println(name+": Something went wrong - "+e.getMessage()+
									". Please try again.");
			}
		}
	}

}