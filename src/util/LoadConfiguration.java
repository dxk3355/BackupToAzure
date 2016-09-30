package util;

import java.io.*;
import java.util.Properties;

public class LoadConfiguration {

	public static Properties getPropValues(String filename){

		Properties prop = new Properties();
		InputStream input = null;

		try {
			input = new FileInputStream(filename);

			// load a properties file
			prop.load(input);
			
			input.close();
			
			return prop;
		} catch (IOException e) {
			System.err.println("Exception: " + e);
		}
		
		return null;

	}
}
