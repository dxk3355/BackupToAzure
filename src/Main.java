import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.*;

import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.azure.storage.*;
import com.microsoft.azure.storage.blob.*;

import util.AzureFile;
import util.LoadConfiguration;

public class Main {
	private final static Logger logger = Logger.getLogger(Main.class.getName());
	public static String storageConnectionString = null;
	public static String storageContainer = null;

	public static void main(String[] args) {
		LinkedList<AzureFile> filesToUpload = new LinkedList<AzureFile>();
		
		Options options = new Options();
		options.addOption("storagePath", true, "Path to prepend to files");
		options.addOption("file", true, "single file to upload");
		options.addOption("propertyFile", true, "Property file with command line argument values");
		options.addOption(Option.builder("filetypes").desc("file types to accept").hasArg().required().build());
		options.addOption(Option.builder("directory").desc("directory to pull from").hasArg().required().build());
		options.addOption(
				Option.builder("storageContainer").desc("Azure storage container to use").hasArg().required().build());
		options.addOption(Option.builder("accountKey").desc("Azure Account Key").hasArg().required().build());
		options.addOption(Option.builder("accountName").desc("Azure Account Name").hasArg().required().build());
		options.addOption(Option.builder("recursive").desc("recursively navigate subdirectories").build());
		options.addOption(Option.builder("overwrite").desc("overwrite existing files if different").build());
		options.addOption(Option.builder("hidden").desc("include hidden files").build());
		options.addOption(Option.builder("force_overwrite").desc("overwrite existing files").build());

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args, false);
		} catch (ParseException e1) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(Main.class.getName() + ".jar", options);
			logger.log(Level.SEVERE, "Invalid command line arguments");
			System.exit(-1);
		}

		boolean recursive = cmd.hasOption("recursive");
		boolean hidden = cmd.hasOption("hidden");
		boolean overwrite = cmd.hasOption("overwrite");
		boolean forceOverwrite = cmd.hasOption("force-overwrite");
		String fileTypes = cmd.getOptionValue("filetypes");
		String storagePath = cmd.getOptionValue("storagePath");
		File directory = new File(cmd.getOptionValue("directory"));

		// Check the directory
		if (!directory.exists() || !directory.isDirectory()) {
			logger.log(Level.SEVERE, "Directory doesn't exist or is not directory.");
			System.exit(-1);
		}

		Pattern pattern = Pattern.compile(fileTypes);
		addFilesFromDirectory(directory, pattern, filesToUpload, directory.getAbsolutePath(), recursive, hidden);
		
		// Prepend the storage directory if it exists
		if (storagePath != null) {
			if (!storagePath.endsWith("\\")) {
				storagePath = storagePath.replace("\\", "/");
			}
			if (!storagePath.endsWith("/")) {
				storagePath = storagePath + "/";
			}
		} else {
			storagePath = "";
		}

		storageConnectionString = "DefaultEndpointsProtocol=https;" + "AccountName=" + cmd.getOptionValue("accountName")
				+ ";" + "AccountKey=" + cmd.getOptionValue("accountKey");
		storageContainer = cmd.getOptionValue("storageContainer");

		int startingSize = filesToUpload.size();
		
		try {
			
			uploadFiles(filesToUpload, storagePath, overwrite, forceOverwrite, recursive);
			
		} catch (InvalidKeyException e) {
			logger.log(Level.SEVERE,"Invalid Azure Key");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE,e.getMessage());
			System.exit(-1);
		} catch (URISyntaxException e) {
			logger.log(Level.SEVERE,e.getMessage());
			System.exit(-1);
		} catch (StorageException e) {
			logger.log(Level.SEVERE,e.getMessage());
			System.exit(-1);
		} catch (IOException e) {
			logger.log(Level.SEVERE,e.getMessage());
			System.exit(-1);
		} catch (NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE,e.getMessage());
			System.exit(-1);
		}

	}
	
	private static void uploadFiles(LinkedList<AzureFile> filesToUpload, String storagePath, boolean overwrite, boolean forceOverwrite, boolean recursive) throws InvalidKeyException, URISyntaxException, StorageException, NoSuchAlgorithmException, IOException{
		// Retrieve storage account from connection-string.
					CloudStorageAccount storageAccount = CloudStorageAccount.parse(storageConnectionString);

					// Create the blob client.
					CloudBlobClient blobClient = storageAccount.createCloudBlobClient();

					// Get a reference to a container.
					// The container name must be lower case
					CloudBlobContainer container = blobClient.getContainerReference(storageContainer);

					AzureFile fileToUpload = null;
					while( filesToUpload.size() > 0 ) {
						fileToUpload = filesToUpload.peek();
						logger.log(Level.FINE,"Uploading: " + fileToUpload.getAbsolutePath());
						
						String azureFilePath = storagePath + fileToUpload.getAzurePath() + fileToUpload.getName();
						
						CloudBlockBlob blob = container.getBlockBlobReference(azureFilePath);
						
						boolean blobExist = blob.exists();
						
						if (blobExist && overwrite) {
							
							// Calculate the file's MD5 and compare the one in Azure
							String md5CheckSum = getMD5ForFile(fileToUpload.getAbsolutePath());
							blob.downloadAttributes();
							BlobProperties blobproperties = blob.getProperties();
							
							if (blobproperties.getContentMD5().equals(md5CheckSum)) {
								// Don't upload if the files match even if overwrite mode is in place
								logger.log(Level.FINER,"File matched existing file: \t" + fileToUpload.getAbsolutePath() + "\t" + azureFilePath);
								
							} else {
								// The files don't match and overwrite mode is enabled so erase then upload new file
								logger.log(Level.FINER,"File differs from existing file: \t" + fileToUpload.getAbsolutePath()+ "\t" + azureFilePath);
								logger.log(Level.FINER,"Deleted: " + blob.deleteIfExists() );
								uploadFile(fileToUpload, blob);
								
							}
						}else if(blobExist && forceOverwrite) {
							// The files don't match and overwrite mode is enabled so erase then upload new file
							logger.log(Level.FINER,"File already exists, overwriting: \t" + fileToUpload.getName()+ "\t" + azureFilePath);
							logger.log(Level.FINER,"Deleted: " + blob.deleteIfExists() );
							uploadFile(fileToUpload, blob);
						}else if( !blobExist ){
							// File doesn't already exist, so just upload
							uploadFile(fileToUpload, blob);
						}else{
							logger.log(Level.WARNING,"File with the same name already exists: \t" + fileToUpload.getAbsolutePath() + "\t" + azureFilePath);
						}
						
						filesToUpload.pop();
					}
	}
	
	/**
	 * Add files to the filesToUpload collection that match the regex
	 * 
	 * @param directory starting directory
	 * @param pattern regular expression
	 * @param filesToUpload collection to add the file to
	 * @param baseDirectoryPath Starting directory for file added
	 * @param recursive should this recursively navigate directories
	 * @param hidden should this include hidden files
	 */
	private static void addFilesFromDirectory(File directory, Pattern pattern, Collection<AzureFile> filesToUpload, String baseDirectoryPath, boolean recursive, boolean hidden){
		Matcher matcher;
		for (File f : directory.listFiles()) {
			if( recursive && f.isDirectory() && (f.isHidden() == hidden)){
				addFilesFromDirectory(f, pattern,  filesToUpload, baseDirectoryPath, true, hidden);
			}
			matcher = pattern.matcher(f.getName());
			if (matcher.matches() && (f.isHidden() == hidden)) {
				filesToUpload.add(new AzureFile(f, baseDirectoryPath ));
			}
		}

	}

	/**
	 * Upload the file to Azure
	 * 
	 * @param fileToUpload File to upload
	 * @param blob Blob object to upload to
	 * @throws FileNotFoundException
	 * @throws StorageException
	 * @throws IOException
	 */
	private static void uploadFile(File fileToUpload, CloudBlockBlob blob) throws FileNotFoundException, StorageException, IOException{
		File source = new File(fileToUpload.getAbsolutePath());
		blob.upload(new FileInputStream(source), source.length());
		
		logger.log(Level.FINE,"Upload Complete: " + fileToUpload.getAbsolutePath());
	}
	
	/**
	 * Generates the MD5 for the input file
	 * 
	 * @param filePath
	 *            Path to the file
	 * @return MD5 as a string
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public static String getMD5ForFile(String filePath) throws NoSuchAlgorithmException, IOException {
		InputStream input = new FileInputStream(filePath);

		byte[] buffer = new byte[1024];
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		int numRead;

		do {
			numRead = input.read(buffer);
			if (numRead > 0) {
				messageDigest.update(buffer, 0, numRead);
			}
		} while (numRead != -1);

		input.close();
		byte[] digest = messageDigest.digest();

		String result = "";

		for (int i = 0; i < digest.length; i++) {
			result += Integer.toString((digest[i] & 0xff) + 0x100, 16).substring(1);
		}
		return result;
	}

}
