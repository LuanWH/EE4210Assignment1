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

/**
 * The abstract class for {@link ClientProcess} and {@link ServerProcess).
 * It implements communication and file transfer methods and defines
 * protocol constants. It extends {@link Thread} class to allow running on
 * a different thread.
 * @author Wenhao
 *
 */
abstract class SyncProcess extends Thread {
	
	/* *********** CONSTANTS FOR PROTOCOL *************** */
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
	/* *****END OF CONSTANTS FOR PROTOCOL *************** */
	
	/*
	 * The standard acknowledgement message
	 */
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

	/**
	 * Inform the process to safely close all connections and exit.
	 * To be called by {@link Peer}.
	 */
	public void close() {
		isClosed = true;
	}

	/**
	 * Check whether this thread is asked to terminate by {@link Peer}.
	 * @return A boolean value telling whether it should close.
	 */
	public boolean isClosed() {
		return this.isClosed;
	}
	
	/**
	 * Send a message to the other {@link Peer}.
	 * @param type The message peer. Must be filled in.
	 * @param name The file name. Can be {@link SyncProcess#NIL}.
	 * @param length The file length. Can be {@link SyncProcess#NIL}.
	 * @return A boolean value telling whether the communication is 
	 * successful ({@code true}) or failed ({@code false}).
	 * @throws IOException If the communication cannot be completed.
	 * @throws ClassNotFoundException If the received acknowledgement is corrupted.
	 */
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
	
	/**
	 * Send an acknowledgement to the other {@link Peer}.
	 * @throws IOException If the communication cannot be completed.
	 */
	protected void sendAck() throws IOException{
		oos.writeObject(ACK);
		oos.flush();
	}
	
	/**
	 * Block the current process and wait to receive an acknowledgement
	 * from the other {@link Peer}.
	 * @return A boolean value telling whether the acknowledgement is successfully 
	 * captured ({@code true}) or failed ({@code false}).
	 * @throws IOException If the communication cannot be completed.
	 * @throws ClassNotFoundException If the received acknowledgement is corrupted.
	 */
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
	
	/**
	 * Get a set of files under the folder to be synced.
	 * @return A {@link Set} of {@link File}
	 */
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

	/**
	 * Extract a set of file names from a set of files.
	 * @param files A {@link Set} of {@link File}.
	 * @return A {@link Set} of file names in {@link String}
	 */
	public Set<String> getFileNameList(Set<File> files) {
		Set<String> fileNameList = new HashSet<String>();
		for (File f : files) {
			fileNameList.add(f.getName());
		}
		return fileNameList;
	}

	/**
	 * Give a set of strings contained in setA but not setB.
	 * @param setA The first input set.
	 * @param setB The second input set.
	 * @return A set of strings.
	 */
	public Set<String> difference(Set<String> setA, Set<String> setB) {
		Set<String> a = new HashSet<String>(setA);
		Set<String> b = new HashSet<String>(setB);
		a.removeAll(b);
		return a;
	}
	
	/**
	 * Send a list of file names to the other {@link Peer}.
	 * @param list The list to be sent.
	 * @return A boolean value telling whether the transfer is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
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
	
	/**
	 * Block and receive a set of file names from the other {@link Peer}.
	 * @return A set of strings representing file names.
	 */
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

	/**
	 * Block and receive a file and write to disk.
	 * @param msg The command sent from the other {@link Peer}.
	 * @return A boolean value telling whether the transfer is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
	protected boolean receiveFile(Vector<String> msg) {
		try {
			if(msg.size() != MSG_SIZE ||
			   msg.get(MSG_NAME_INDEX).equals(NIL) ||
			   msg.get(MSG_LENGTH_INDEX).equals(NIL)){
				return false;
			}
			
			long size = Long.valueOf(msg.get(MSG_LENGTH_INDEX));

			sendAck();
			
			//Read from the other Peer
			byte[] bytes = new byte[(int) size];
			ois.read(bytes);
			
			//Write to a file on disk
			File fout = new File(folder.getPath() + File.separator + msg.get(MSG_NAME_INDEX));
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

	/**
	 * Send a file to the other {@link Peer}.
	 * @param fileName The name of the file to be sent.
	 * @return A boolean value telling whether the transfer is 
	 * successful ({@code true}) or failed ({@code false}).
	 */
	protected boolean pushFile(String fileName){
		try{
			//Validate the file on disk
			File fin = new File(folder.getPath() + File.separator + fileName);
			if (!fin.exists()) {
				throw new IOException("File " + fin.getName() + " not found!");
			}
			long size = fin.length();			
			
			//Sending file information to the other peer
			boolean success = false;
			success = sendMsg(TYPE_PUSH, fileName, String.valueOf(size));
			if(!success) return false;
			
			//Load the file from disk
			byte[] bytes = Files.readAllBytes(fin.toPath());
			
			//Send the file data to the other peer
			oos.write(bytes);
			oos.flush();

			return recvAck();
		} catch (IOException | ClassNotFoundException e) {
			System.out.println(name + ": " + e.getMessage());
			return false;
		}
	}
	
}