package syncpeer;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

abstract class SyncProcess extends Thread {

	public static final int TIME_OUT = 100;

	public String name;
	protected int port;
	protected File folder;
	protected String ipAddr;
	protected boolean isClosed;

	public void close(){
		isClosed = true;
	}

	public boolean isClosed() {
		return this.isClosed;
	}

	public Set<File> getFileList() {
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
	
	public Set<String> difference(Set<String> setA, Set<String> setB){
		Set<String> a = new HashSet<String>(setA);
		Set<String> b = new HashSet<String>(setB);
		a.removeAll(b);
		return a;
	}
}