# BackupToAzure
Java based backup tool to backup data to Azure storage.

After you download the files you can build the files into a jar.  The entry point is main.java.com.backuptoazure.Main. 

You can run the tool with a combination of command line and property file settings.  

    usage: main.java.com.backuptoazure.Main.class
     -accountKey <arg>         Azure Account Key
     -accountName <arg>        Azure Account Name
     -archive                  toggle archive bit
     -directory <arg>          directory to pull from
     -file <arg>               single file to upload
     -filetypes <arg>          file types to accept
     -force_overwrite          overwrite existing files
     -hidden                   include hidden files
     -overwrite                overwrite existing files if different
     -propertyFile <arg>       Property file with command line argument values
     -recursive                recursively navigate subdirectories
     -storageContainer <arg>   Azure storage container to use
     -storagePath <arg>        Path to prepend to files
 
 A property file would look like this.  An example property file is included in the resources directory and test directories.
 
    accountName=
    accountKey=
    storageContainer=test
    filetypes=([^\s]+(\.(?i)(7z|rar|txt|db))(\.[0-9]+)$)
    directory=C:\\ftp
