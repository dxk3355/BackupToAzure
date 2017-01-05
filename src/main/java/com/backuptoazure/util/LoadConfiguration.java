package main.java.com.backuptoazure.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class LoadConfiguration {

	/**
	 * Loads the properties
	 * 
	 * @param filename Path to properties file
	 * @return Properties class
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	public static Properties getPropValues(String filename) throws IOException, FileNotFoundException {

		Properties prop = new Properties();
		InputStream input = null;

		
		input = new FileInputStream(filename);

		try{
		
		// load a properties file
		prop.load(input);
		
		}finally{
			input.close();
		}
		
		return prop;

	}

}
