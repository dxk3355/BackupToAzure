package main.java.com.backuptoazure.model;

import java.io.File;
import java.net.URI;

public class AzureFile extends File {

	String baseDirectory;
	
	/**
	 * 
	 * @param pathname Path to the file
	 * @param baseDirectory Starting directory for file added
	 */
	public AzureFile(String pathname, String baseDirectory) {
		super(pathname);
		this.baseDirectory = baseDirectory;
	}

/**
 * 
 * @param uri URI to file
 * @param baseDirectory Starting directory for file added
 */
	public AzureFile(URI uri, String baseDirectory) {
		super(uri);
		this.baseDirectory = baseDirectory;
	}
	
	/**
	 * 
	 * @param f File reference to file
	 * @param baseDirectory Starting directory for file added
	 */
	public AzureFile(File f, String baseDirectory){
		super(f.getPath());
		this.baseDirectory = baseDirectory;
	}


	/**
	 * Gets the difference between the parent path and the base directory to determine the path to use for Azure storage and converts to a compatible Azure path.
	 * @return Path for Azure storage not including the filename
	 */
	public String getAzurePath(){
		String parent = this.getParent() + "\\";
		String path = parent.substring(baseDirectory.length() + 1, parent.length() );
		path = path.replace("\\", "/");
		return path;
	}
}
