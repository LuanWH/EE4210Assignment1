package syncpeer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

abstract class SyncProcess extends Thread {

	public static final String TYPE_REQUEST = "REQUEST";
	public static final String TYPE_PUSH = "PUSH";
	public static final String TYPE_SYNC = "SYNC";
	public static final String TYPE_ACK = "ACK";
	public static final String NIL = "";
		
	public static final int MSG_SIZE = 3;
	public static final int MSG_TYPE_INDEX = 0;
	public static final int MSG_NAME_INDEX = 1;
	public static final int MSG_LENGTH_INDEX = 2;

	public static final int FILE_LIST_LENGTH = 2;
	public static final int MISSING_FILE_LIST_INDEX = 0;
	public static final int EXTRA_FILE_LIST_INDEX = 1;
	
	public static final Vector<String> ACK;
	static{
		ACK = new Vector<String>();
		ACK.setSize(MSG_SIZE);
		ACK.set(MSG_TYPE_INDEX, TYPE_ACK);
		ACK.set(MSG_NAME_INDEX, NIL);
		ACK.set(MSG_LENGTH_INDEX, NIL);
	}
	
	public static final int TIME_OUT = 300;
	public static final int BUFFER_SIZE = 4096;
	
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
	
	protected boolean sendMsg(String type, String name, String length) throws IOException, ClassNotFoundException{
		Vector<String> message = new Vector<String>();
		message.setSize(MSG_SIZE);
		message.set(MSG_TYPE_INDEX, type);
		message.set(MSG_NAME_INDEX, name);
		message.set(MSG_LENGTH_INDEX, length);
		oos.writeObject(message);
		oos.flush();
		return recvAck();
	}
	
	protected void sendAck() throws IOException{
		oos.writeObject(ACK);
		oos.flush();
	}
	
	@SuppressWarnings("unchecked")
	protected boolean recvAck() throws ClassNotFoundException, IOException{
		Vector<String> response = (Vector<String>) ois.readObject();
		if (response.size() != MSG_SIZE || 
			!response.get(MSG_TYPE_INDEX).equalsIgnoreCase(TYPE_ACK)){
			return false;
		} else {
			return true;
		}
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
			if (!recvAck()) {
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
			sendAck();
			return list;
		} catch (IOException | ClassNotFoundException e){
			System.out.println(name + ": " + e.getMessage());
			return null;
		}
	}	

	protected boolean receiveFile(Vector<String> msg) {
		try {
			if(msg.size() != MSG_SIZE ||
			   msg.get(MSG_NAME_INDEX).equals(NIL) ||
			   msg.get(MSG_LENGTH_INDEX).equals(NIL)){
				return false;
			}
			
			File fout = new File(folder.getPath() + File.separator + msg.get(MSG_NAME_INDEX));
			long size = Long.valueOf(msg.get(MSG_LENGTH_INDEX));

			sendAck();
			
			byte[] bytes = new byte[(int) size];
			ois.read(bytes);
			FileOutputStream fos = new FileOutputStream(fout);
			fos.write(bytes);
			fos.close();
			
			sendAck();
			return true;
		} catch (IOException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}

	protected boolean pushFile(String fileName){
		try{
			File fin = new File(folder.getPath() + File.separator + fileName);
			if (!fin.exists()) {
				throw new IOException("File " + fin.getName() + " not found!");
			}
			long size = fin.length();			
			
			boolean success = false;
			success = sendMsg(TYPE_PUSH, fileName, String.valueOf(size));
			if(!success) return false;
			
			byte[] bytes = Files.readAllBytes(fin.toPath());
			
			oos.write(bytes);
			oos.flush();

			return recvAck();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}
	
}