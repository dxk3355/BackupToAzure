package main.java.com.backuptoazure;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.microsoft.azure.storage.StorageException;

import main.java.com.backuptoazure.model.AzureFile;
import main.java.com.backuptoazure.util.LoadConfiguration;

public class Main {
	private final static Logger logger = Logger.getLogger(Main.class.getName());
	public static String storageConnectionString = null;
	public static String storageContainer = null;

	public static void main(String[] args) {
		LinkedList<AzureFile> filesToUpload = new LinkedList<AzureFile>();

		// First we are going to check for a properties file to be specified
		// If there is a properties file we will load that and use it for
		// variables
		Properties propertiesFileContents = null;

		Options properties = getOptionsForPropertyFile();
		DefaultParser parser = new DefaultParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(properties, args, false);
			String filename = cmd.getOptionValue("propertyFile");
			if (filename != null) {
				propertiesFileContents = LoadConfiguration.getPropValues(filename);
			}
		} catch (UnrecognizedOptionException e) {
			// Ignore this exception since there might be extra options
		} catch (ParseException e) {
			logger.log(Level.SEVERE, "Unable to find properties file");
			System.exit(-1);
		} catch (FileNotFoundException e) {
			logger.log(Level.SEVERE, "Unable to find properties file");
			System.exit(-1);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Unable to parse properties file");
			System.exit(-1);
		}

		Options options = getOptions();

		try {
			if (propertiesFileContents != null) {
				cmd = parser.parse(options, args, propertiesFileContents, false);
			} else {
				cmd = parser.parse(options, args, false);
			}
		} catch (ParseException e) {
			e.printStackTrace();
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
		storagePath = storagePath(storagePath);

		storageConnectionString = BackupController.connectionString(cmd.getOptionValue("accountName"),
				cmd.getOptionValue("accountKey"));
		storageContainer = cmd.getOptionValue("storageContainer");

		try {

			BackupController backupController = new BackupController(storageContainer, storageConnectionString,
					storagePath);
			backupController.uploadFiles(filesToUpload, overwrite, forceOverwrite);

		} catch (InvalidKeyException e) {
			logger.log(Level.SEVERE, "Invalid Azure Key");
			System.exit(-1);
		} catch (URISyntaxException | StorageException | IOException | NoSuchAlgorithmException e) {
			logger.log(Level.SEVERE, e.getMessage());
			System.exit(-1);
		}

	}

	/**
	 * Add files to the filesToUpload collection that match the regex
	 * 
	 * @param directory
	 *            starting directory
	 * @param pattern
	 *            regular expression
	 * @param filesToUpload
	 *            collection to add the file to
	 * @param baseDirectoryPath
	 *            Starting directory for file added
	 * @param recursive
	 *            should this recursively navigate directories
	 * @param hidden
	 *            should this include hidden files
	 */
	private static void addFilesFromDirectory(File directory, Pattern pattern, Collection<AzureFile> filesToUpload,
			String baseDirectoryPath, boolean recursive, boolean hidden) {
		Matcher matcher;
		for (File f : directory.listFiles()) {
			if (recursive && f.isDirectory()) {
				if (!f.isHidden()) {
					addFilesFromDirectory(f, pattern, filesToUpload, baseDirectoryPath, true, hidden);
				} else if (f.isHidden() == hidden) {
					addFilesFromDirectory(f, pattern, filesToUpload, baseDirectoryPath, true, hidden);
				}
			}
			matcher = pattern.matcher(f.getName());
			if (matcher.matches()) {
				if (!f.isHidden()) {
					filesToUpload.add(new AzureFile(f, baseDirectoryPath));
				} else if (f.isHidden() == hidden) {
					filesToUpload.add(new AzureFile(f, baseDirectoryPath));
				}
			}
		}

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

	private static Options getOptions() {
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
		options.addOption(
				Option.builder("recursive").desc("recursively navigate subdirectories").optionalArg(true).build());
		options.addOption(
				Option.builder("overwrite").desc("overwrite existing files if different").optionalArg(true).build());
		options.addOption(Option.builder("hidden").desc("include hidden files").optionalArg(true).build());
		options.addOption(Option.builder("archive").desc("toggle archive bit").optionalArg(true).build());
		options.addOption(Option.builder("force_overwrite").desc("overwrite existing files").optionalArg(true).build());
		return options;
	}

	private static Options getOptionsForPropertyFile() {
		Options options = new Options();
		options.addOption("storagePath", true, "Path to prepend to files");
		options.addOption("file", true, "single file to upload");
		options.addOption("propertyFile", true, "Property file with command line argument values");
		options.addOption(Option.builder("filetypes").desc("file types to accept").hasArg().build());
		options.addOption(Option.builder("directory").desc("directory to pull from").hasArg().build());
		options.addOption(Option.builder("storageContainer").desc("Azure storage container to use").hasArg().build());
		options.addOption(Option.builder("accountKey").desc("Azure Account Key").hasArg().build());
		options.addOption(Option.builder("accountName").desc("Azure Account Name").hasArg().build());
		options.addOption(
				Option.builder("recursive").desc("recursively navigate subdirectories").optionalArg(true).build());
		options.addOption(
				Option.builder("overwrite").desc("overwrite existing files if different").optionalArg(true).build());
		options.addOption(Option.builder("hidden").desc("include hidden files").optionalArg(true).build());
		options.addOption(Option.builder("archive").desc("toggle archive bit").optionalArg(true).build());
		options.addOption(Option.builder("force_overwrite").desc("overwrite existing files").optionalArg(true).build());
		return options;
	}

	/**
	 * Prepend the storage directory if it exists
	 * 
	 * @param storagePath
	 * @return
	 */
	private static String storagePath(String storagePath) {
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
		return storagePath;
	}

}
