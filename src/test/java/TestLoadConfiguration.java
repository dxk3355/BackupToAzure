package test.java;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

public class TestLoadConfiguration {

	@Test
	public void testBasicTest() {
		String filePath = "../BackupToAzure/src/test/resources/properties/BasicTest.properties";
		
		try {
			Properties properties = main.java.com.backuptoazure.util.LoadConfiguration.getPropValues(filePath);
			assert(properties.getProperty("accountName").equals("ExampleName"));
			assert(properties.getProperty("accountname") == null);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
	}
	
	@Test
	public void testMissingData() {
		String filePath = "../BackupToAzure/src/test/resources/properties/MissingData.properties";
		
		try {
			Properties properties = main.java.com.backuptoazure.util.LoadConfiguration.getPropValues(filePath);
			assert(properties.getProperty("accountName").equals(""));
			assert(properties.getProperty("accountKey") == null);
			
		} catch (IOException e) {
			fail(e.getMessage());
		}
		
	}

}
