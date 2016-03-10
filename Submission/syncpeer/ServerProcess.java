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

/**
 * A {@link Thread} class that listens to socket connections and handles 
 * remote client synchronization commands. It will close socket connection to a connected
 * peer once file synchronization is done and then return to waiting status.
 * It keeps running once started and exits only when the {@link ServerProcess#close()} 
 * method is called or the thread is terminated.
 * @author Wenhao
 *
 */
class ServerProcess extends SyncProcess {

	private static final String SERVER_PROCESS_NAME = "ServerProcess";

	private ServerSocket socket;

	ServerProcess(File folder, int port) {
		this.folder = folder;
		this.port = port;
		this.name = SERVER_PROCESS_NAME;
		this.isClosed = false;
	}

	/**
	 * Handle client file request. Will call {@link ServerProcess#pushFile(String)} to
	 * send the requested file.
	 * @param request The request command from client.
	 * @return A boolean value telling whether the handling is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
	private boolean requestHandler(Vector<String> request){
		try{
			//Validate and acknowledge the received command.
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
	
	/**
	 * Handle client request of pushing file. Will call {@link ServerProcess#receiveFile(Vector)} to
	 * receive the incoming file from client.
	 * @param fileInfo The file information sent by the client.
	 * @return A boolean value telling whether the handling is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
	private boolean pushHandler(Vector<String> fileInfo){
		System.out.println(name + ": receive file "+fileInfo.get(MSG_NAME_INDEX));
		return receiveFile(fileInfo);
	}
	
	/**
	 * Handle client request of synchronization of file name lists. It receives a file name list from
	 * client and compares to its own file name lists. It sends back to the client two lists of file
	 * names where the first list contains the file names the client is missing and the second contains
	 * the file names the server is missing.
	 * @return A boolean value telling whether the handling is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
	private boolean syncHandler(){
		try{
			//Acknowledge the received command.
			sendAck();
			
			//Construct file name lists in sync folder
			Set<File> fileList = getFileList();
			Set<String> fileNameList = getFileNameList(fileList);

			//Receive file name lists from client
			Set<String> clientFileNameList = receiveFileList();
			if(clientFileNameList == null) {
				System.out.println("Unable to sync file lists.");
				return false;
			}

			//Obtain the differences between local list and client list
			Set<String> clientMissingFileNameList = difference(
					fileNameList, clientFileNameList);
			Set<String> clientExtraFileNameList = difference(
					clientFileNameList, fileNameList);
			
			//Send difference results back to client.
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
	
	/**
	 * Receive remote commands from client. A simple validity checking is performed.
	 * @return The client command received if it is valid, or {@code null} if invalid.
	 * @throws IOException If the socket connection failed.
	 */
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
	
	/**
	 * Dispatch the user command read from {@link ServerProcess#readCommand()}.
	 * It will check type of the command and dispatch to its respective handler.
	 * @param cmd The user command.
	 * @return A boolean value telling whether the handling is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
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
	
	/**
	 * Start the server thread and keeps waiting for socket connection.
	 * It scans user inputs and dispatch to respective handlers.
	 * It keeps running once started and exits only when the {@link ServerProcess#close()} 
	 * method is called or the thread is terminated.
	 */
	@Override
	public void run() {
		//Looping for synchronization sessions
		while (!isClosed()) {
			try {
				//Waiting until the port can be bind.
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

				//Waiting for client connections
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
				
				//Establish connection socket streams
				fromClientSocket.setSoTimeout(TIME_OUT*5);
				oos = new ObjectOutputStream(
						fromClientSocket.getOutputStream());
				ois = new ObjectInputStream(
						fromClientSocket.getInputStream());
				
				//Looping for user commands.
				Vector<String> cmd;
				while(!isClosed() && !fromClientSocket.isClosed()){
					cmd = readCommand();
					if(cmd == null) {
						System.out.println(name+": Invalid command from the other peer!");
						continue;
					}
					dispatchCommand(cmd);
				}

				//Clean up connections and prepare for next synchronization session.
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