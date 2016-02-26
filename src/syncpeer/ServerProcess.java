package syncpeer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
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
					if(socket != null) socket.close();
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
					if(socket != null) socket.close();
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

				System.out.println("Client Missing files:");
				for (String s : clientMissingFileNameList) {
					System.out.println(s);
					File fin = new File(folder.getPath()+File.separator+s);
					if(!fin.exists()){
						throw new IOException("File "+fin.getName()+" not found!");
					}
					long size = fin.length();
					oos.writeLong(size);
					byte[] bytes = Files.readAllBytes(fin.toPath());
					oos.write(bytes);
				}
				
				System.out.println();
				
				System.out.println("Client Extra files:");
				for (String s : clientExtraFileNameList) {
					System.out.println(s);
					File fout = new File(folder.getPath()+File.separator+s);
					long size = ois.readLong();
					byte[] bytes = new byte[(int) size];
					ois.read(bytes);
					FileOutputStream fos = new FileOutputStream(fout);
					fos.write(bytes);
					fos.close();
				}

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