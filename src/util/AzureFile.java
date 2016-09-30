package util;

import java.io.File;
import java.net.URI;

public class AzureFile extends File {

	String baseDirectory;
	
	public AzureFile(String pathname, String baseDirectory) {
		super(pathname);
		this.baseDirectory = baseDirectory;
	}

	public AzureFile(URI uri, String baseDirectory) {
		super(uri);
		this.baseDirectory = baseDirectory;
	}
	
	public AzureFile(File f, String baseDirectory){
		super(f.getPath());
		this.baseDirectory = baseDirectory;
	}


	public String getAzurePath(){
		String parent = this.getParent() + "\\";
		String path = parent.substring(baseDirectory.length() + 1, parent.length() );
		path = path.replace("\\", "/");
		return path;
	}
}
