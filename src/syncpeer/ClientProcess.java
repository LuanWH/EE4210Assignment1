package syncpeer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Set;

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

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		try {
			if (ipAddr == null) {
				System.out.println("ipAddr not set!");
				return;
			}

			socket = new Socket(ipAddr, port);
			
			System.out.println(name+": Connection established.");

			Set<File> fileList = getFileList();
			Set<String> fileNameList = getFileNameList(fileList);

			ObjectInputStream ois = new ObjectInputStream(
					socket.getInputStream());
			ObjectOutputStream oos = new ObjectOutputStream(
					socket.getOutputStream());

			oos.writeObject(fileNameList);

			Set<String> missingFileNameList = (Set<String>) ois.readObject();
			Set<String> extraFileNameList = (Set<String>) ois.readObject();

			System.out.println("Missing files:");
			for (String s : missingFileNameList) {
				System.out.println(s);
				File fout = new File(folder.getPath()+File.separator+s);
				long size = ois.readLong();
				byte[] bytes = new byte[(int) size];
				ois.read(bytes);
				FileOutputStream fos = new FileOutputStream(fout);
				fos.write(bytes);
				fos.close();
			}

			System.out.println();

			System.out.println("Extra files:");
			for (String s : extraFileNameList) {
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

			String ack = "Received!\n";
			oos.writeObject(ack);

			ois.close();
			oos.close();

			socket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}