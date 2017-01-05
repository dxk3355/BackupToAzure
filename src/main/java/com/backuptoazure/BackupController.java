package main.java.com.backuptoazure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.BlobProperties;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import main.java.com.backuptoazure.model.AzureFile;

public class BackupController {
	
	private final static Logger logger = Logger.getLogger(Main.class.getName());
	private String storagePath;
	private String storageContainer;
	private String storageConnectionString;
	
	/**
	 * 
	 * @param storageContainer
	 * @param storageConnectionString
	 * @param storagePath
	 * @throws IllegalArgumentException
	 */
	public BackupController(String storageContainer, String storageConnectionString, String storagePath) throws IllegalArgumentException{
		if( storageContainer == null || storageContainer == ""  ){
			throw new IllegalArgumentException("Storage container cannot be empty or null");
		}
		if( storageConnectionString == null || storageConnectionString == "" ){
			throw new IllegalArgumentException("ConnectionString cannot be empty or null");
		}
		if( storagePath == null ){
			storagePath = "";
		}
		this.storagePath = storagePath;
		this.storageContainer = storageContainer;
		this.storageConnectionString = storageConnectionString;
	}
	

	/**
	 * 
	 * @param filesToUpload List of azure files to upload
	 * @param overwrite flag to indicate to overwrite existing files based on name
	 * @param forceOverwrite flag to indicate to overwrite existing files
	 * @throws InvalidKeyException
	 * @throws URISyntaxException
	 * @throws StorageException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public void uploadFiles(LinkedList<AzureFile> filesToUpload, boolean overwrite,
			boolean forceOverwrite)
			throws InvalidKeyException, URISyntaxException, StorageException, NoSuchAlgorithmException, IOException {
		// Retrieve storage account from connection-string.
		CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

		// Create the blob client.
		CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

		// Get a reference to a container.
		// The container name must be lower case
		CloudBlobContainer container = blobClient.getContainerReference(storageContainer);

		AzureFile fileToUpload = null;
		while (filesToUpload.size() > 0) {
			fileToUpload = filesToUpload.peek();
			logger.log(Level.FINE, "Uploading: " + fileToUpload.getAbsolutePath());

			String azureFilePath = storagePath + fileToUpload.getAzurePath() + fileToUpload.getName();

			CloudBlockBlob blob = container.getBlockBlobReference(azureFilePath);

			boolean blobExist = blob.exists();

			if (blobExist && overwrite) {

				// Calculate the file's MD5 and compare the one in Azure
				String md5CheckSum = Main.getMD5ForFile(fileToUpload.getAbsolutePath());
				blob.downloadAttributes();
				BlobProperties blobproperties = blob.getProperties();

				if (blobproperties.getContentMD5().equals(md5CheckSum)) {
					// Don't upload if the files match even if overwrite mode is
					// in place
					logger.log(Level.FINER,
							"File matched existing file: \t" + fileToUpload.getAbsolutePath() + "\t" + azureFilePath);

				} else {
					// The files don't match and overwrite mode is enabled so
					// erase then upload new file
					logger.log(Level.FINER, "File differs from existing file: \t" + fileToUpload.getAbsolutePath()
							+ "\t" + azureFilePath);
					logger.log(Level.FINER, "Deleted: " + blob.deleteIfExists());
					uploadFile(fileToUpload, blob);

				}
			} else if (blobExist && forceOverwrite) {
				// The files don't match and overwrite mode is enabled so erase
				// then upload new file
				logger.log(Level.FINER,
						"File already exists, overwriting: \t" + fileToUpload.getName() + "\t" + azureFilePath);
				logger.log(Level.FINER, "Deleted: " + blob.deleteIfExists());
				uploadFile(fileToUpload, blob);
			} else if (!blobExist) {
				// File doesn't already exist, so just upload
				uploadFile(fileToUpload, blob);
			} else {
				logger.log(Level.WARNING, "File with the same name already exists: \t" + fileToUpload.getAbsolutePath()
						+ "\t" + azureFilePath);
			}

			filesToUpload.pop();
		}
	}
	
	/**
	 * Upload the file to Azure
	 * 
	 * @param fileToUpload
	 *            File to upload
	 * @param blob
	 *            Blob object to upload to
	 * @throws FileNotFoundException
	 * @throws StorageException
	 * @throws IOException
	 */
	private void uploadFile(File fileToUpload, CloudBlockBlob blob)
			throws FileNotFoundException, StorageException, IOException {
		File source = new File(fileToUpload.getAbsolutePath());
		blob.upload(new FileInputStream(source), source.length());

		logger.log(Level.FINE, "Upload Complete: " + fileToUpload.getAbsolutePath());
	}
	
	public static String connectionString(String accountName, String accountKey) throws IllegalArgumentException{
		if( accountName == null || accountName == ""  ){
			throw new IllegalArgumentException("Account Name cannot be empty or null");
		}
		if( accountKey == null || accountKey == "" ){
			throw new IllegalArgumentException("Account Key cannot be empty or null");
		}
		return "DefaultEndpointsProtocol=https;" + "AccountName=" + accountName + ";" + "AccountKey=" + accountKey;
	}

}
