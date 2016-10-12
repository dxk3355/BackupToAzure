package test.java;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import main.java.com.backuptoazure.Main;

public class TestMain {

	@Test
	public void testMain() {
		String[] args = {"",""};

		Main.main(args);

	}

	@Test
	public void testGetMD5ForFile() {
		String filePath = "../BackupToAzure/src/test/resources/a8cb2b8c013edc5d843daac7820d9337.jpg";
		
		try {
			String md5Result = Main.getMD5ForFile(filePath);
			assert(md5Result.equals("a8cb2b8c013edc5d843daac7820d9337"));
		} catch (NoSuchAlgorithmException | IOException e) {
			fail(e.getMessage());
		}
		
	}

}
