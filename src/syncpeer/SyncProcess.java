package syncpeer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

abstract class SyncProcess extends Thread {

	public static final String REQUEST_FILE = "REQUEST";
	public static final String ACK_REQUEST = "ACK_REQUEST";
	public static final String PUSH_FILE = "PUSH";
	public static final String ACK_PUSH = "ACK_PUSH";
	public static final String SYNC_FILE_LIST = "SYNC_FILE_LIST";
	public static final String ACK_SYNC = "ACK_SYNC";
	
	public static final String ACK_NAME = "ACK_NAME";
	public static final String ACK_LENGTH = "ACK_LENGTH";
	public static final String ACK_RECV = "ACK_RECV";

	public static final int TIME_OUT = 300;
	public static final int BUFFER_SIZE = 4096;
	
	public static final int FILE_LIST_LENGTH = 2;
	public static final int MISSING_FILE_LIST_INDEX = 0;
	public static final int EXTRA_FILE_LIST_INDEX = 1;

	public String name;
	protected int port;
	protected File folder;
	protected String ipAddr;
	protected boolean isClosed;

	protected ObjectOutputStream oos;
	protected ObjectInputStream ois;

	public void close() {
		isClosed = true;
	}

	public boolean isClosed() {
		return this.isClosed;
	}

	protected Set<File> getFileList() {
		File[] listOfFiles = folder.listFiles();
		Set<File> files = new HashSet<File>();

		for (File file : listOfFiles) {
			if (file.exists() && file.isFile()) {
				files.add(file);
			}
		}
		return files;
	}

	public Set<String> getFileNameList(Set<File> files) {
		Set<String> fileNameList = new HashSet<String>();
		for (File f : files) {
			fileNameList.add(f.getName());
		}
		return fileNameList;
	}

	public Set<String> difference(Set<String> setA, Set<String> setB) {
		Set<String> a = new HashSet<String>(setA);
		Set<String> b = new HashSet<String>(setB);
		a.removeAll(b);
		return a;
	}
	
	protected boolean sendFileList(Set<String> list){
		try{
			if(list == null){
				return false;
			}
			oos.writeObject(list);
			oos.flush();
			String ack = (String) ois.readObject();
			if (!ack.equalsIgnoreCase(ACK_RECV)) {
				return false;
			}		
			return true;
		} catch (IOException | ClassNotFoundException e){
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected Set<String> receiveFileList(){
		try{
			Set<String> list = (Set<String>) ois.readObject();
			oos.writeObject(ACK_RECV);
			oos.flush();
			return list;
		} catch (IOException | ClassNotFoundException e){
			System.out.println(name + ": " + e.getMessage());
			return null;
		}
	}	

	protected boolean receiveFile(String fileName) {
		try {
			File fout = new File(folder.getPath() + File.separator + fileName);
			long size = ois.readLong();
			oos.writeObject(ACK_LENGTH);
			oos.flush();
			
			byte[] bytes = new byte[(int) size];
			ois.read(bytes);
			FileOutputStream fos = new FileOutputStream(fout);
			fos.write(bytes);
			fos.close();
			
			oos.writeObject(ACK_RECV);
			oos.flush();
			
			return true;
		} catch (IOException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}

	protected boolean sendFile(String fileName){
		try{

			File fin = new File(folder.getPath() + File.separator + fileName);
			if (!fin.exists()) {
				throw new IOException("File " + fin.getName() + " not found!");
			}
			long size = fin.length();
			oos.writeLong(size);
			oos.flush();
			
			String lengthAck = (String) ois.readObject();
			if (!lengthAck.equalsIgnoreCase(ACK_LENGTH)) {
				return false;
			}
			
			byte[] bytes = Files.readAllBytes(fin.toPath());
			oos.write(bytes);
			oos.flush();
			
			String recvAck = (String) ois.readObject();
			if (!recvAck.equalsIgnoreCase(ACK_RECV)) {
				return false;
			}
			return true;
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}
	
}