package syncpeer;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Set;

class ServerProcess extends SyncProcess {

	private static final String ServerProcessName = "ServerProcess";

	private ServerSocket socket;

	ServerProcess(File folder, int port) {
		this.folder = folder;
		this.port = port;
		this.name = ServerProcessName;
		this.isClosed = false;
	}

	@SuppressWarnings("unchecked")
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
					socket.close();
					return;
				}
				System.out.println(name + ": port bind successful.");

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
					socket.close();
					return;
				}
				System.out.println(name + ": connection established.");

				Set<File> fileList = getFileList();
				Set<String> fileNameList = getFileNameList(fileList);

				ObjectOutputStream oos = new ObjectOutputStream(
						fromClientSocket.getOutputStream());
				ObjectInputStream ois = new ObjectInputStream(
						fromClientSocket.getInputStream());

				Set<String> clientFileNameList = (Set<String>) ois.readObject();

				Set<String> clientMissingFileNameList = difference(
						fileNameList, clientFileNameList);
				Set<String> clientExtraFileNameList = difference(
						clientFileNameList, fileNameList);

				oos.writeObject(clientMissingFileNameList);
				oos.writeObject(clientExtraFileNameList);

				String ack = (String) ois.readObject();
				System.out.println("Received from client: " + ack);

				oos.close();
				ois.close();
				fromClientSocket.close();
				socket.close();

			} catch (IOException | ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}

}