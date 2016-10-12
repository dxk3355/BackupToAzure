package main.java.com.backuptoazure.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

public class LoadConfiguration {

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
