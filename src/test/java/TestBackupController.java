package test.java;

import static org.junit.Assert.fail;

import org.junit.Test;

public class TestBackupController {

	@Test(expected=IllegalArgumentException.class)
	public void testBackupControllerEmptyStorageContainerString() {
		main.java.com.backuptoazure.BackupController bc = new main.java.com.backuptoazure.BackupController("","test","test");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testBackupControllerEmptyStorageConnectionString() {
		main.java.com.backuptoazure.BackupController bc = new main.java.com.backuptoazure.BackupController("test","","test");
	}

	public void testBackupControllerEmptyStoragePathString() {
		main.java.com.backuptoazure.BackupController bc = new main.java.com.backuptoazure.BackupController("test","test","");
	}

	
	@Test(expected=IllegalArgumentException.class)
	public void testBackupControllerNullStorageContainerString() {
		main.java.com.backuptoazure.BackupController bc = new main.java.com.backuptoazure.BackupController(null,"test","test");
	}

	
	@Test(expected=IllegalArgumentException.class)
	public void testBackupControllerNullStorageConnectionString() {
		main.java.com.backuptoazure.BackupController bc = new main.java.com.backuptoazure.BackupController("test",null,"test");
	}

	public void testBackupControllerNullStoragePathString() {
		main.java.com.backuptoazure.BackupController bc = new main.java.com.backuptoazure.BackupController("test","test",null);
	}


	@Test
	public void testUploadFiles() {	
		fail("Not yet implemented");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConnectionStringEmptyAccountNameString() {
		main.java.com.backuptoazure.BackupController.connectionString("", "test");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConnectionStringNullAccountNameString() {
		main.java.com.backuptoazure.BackupController.connectionString(null, "test");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConnectionStringEmptyAccountKeyString() {
		main.java.com.backuptoazure.BackupController.connectionString("test", "");
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConnectionStringNullAccountKeyString() {
		main.java.com.backuptoazure.BackupController.connectionString("test", null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConnectionStringNullStrings() {
		main.java.com.backuptoazure.BackupController.connectionString(null, null);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testConnectionStringEmptyStrings() {
		main.java.com.backuptoazure.BackupController.connectionString("", "");
	}

}
