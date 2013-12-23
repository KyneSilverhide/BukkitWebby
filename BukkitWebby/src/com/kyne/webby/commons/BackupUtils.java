package com.kyne.webby.commons;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.bukkit.Server;
import org.bukkit.World;


public class BackupUtils {

	private final static SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyyMMdd_HHmmss");

	public static void startBukkitBackup(final Server server) {
		startBackup(server, BackupMode.BUKKIT, null);
	}
	
	public static void startSMPBackup(final Server server, final String smpWorldName) {
		startBackup(server, BackupMode.SMP, smpWorldName);
	}
	
	public static void startBackup(final Server server, final BackupMode mode, final String smpWorldName) {
		final String rootDir = "Backups/";
		final String backupUniqueId = sdf.format(new Date());
		final String backupDirectory = rootDir + backupUniqueId;
		final boolean dirCreated = new File(backupDirectory).mkdirs();
		if (!dirCreated) {
			LogHelper.error("Unable to create the backup directory "
					+ backupDirectory + ". Backup will be skipped. ");
		} else {
			LogHelper.info("Backup worlds (mode = " + mode.name() + ") ...");
			if(mode == BackupMode.SMP) {
				LogHelper.info("Using default world name : " + smpWorldName);
				try {
					FileUtils.copyDirectoryToDirectory(new File(smpWorldName), new File(backupDirectory));
				} catch (IOException e) {
					LogHelper.error("Unable to backup world " + smpWorldName, e); 
				}
			} else if(mode == BackupMode.BUKKIT) {
				final List<World> worlds = new ArrayList<World>(server.getWorlds());
				for (final World world : worlds) {
					final String worldName = world.getName();
					try {
						FileUtils.copyDirectoryToDirectory(new File(worldName), new File(backupDirectory));
					} catch (IOException e) {
						LogHelper.error("Unable to backup world " + worldName, e); 
					}
				}
			}
			LogHelper.info("Done.");
			LogHelper.info("Compressing backup...");
			try {
				zipDirectory(backupDirectory, backupDirectory + ".zip");
				FileUtils.deleteDirectory(new File(backupDirectory));
			} catch (FileNotFoundException e) {
				LogHelper.error("Unable to find the backup directory " + backupDirectory, e);
			} catch (IOException e) {
				LogHelper.warn("Unable to deleted the uncompressed backup directory");
			}
			LogHelper.info("Done.");
		}
	}

	public static void zipDirectory(final String srcFolder, final String destZipFile)
			throws FileNotFoundException {
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;
		try {
			fileWriter = new FileOutputStream(destZipFile);
			zip = new ZipOutputStream(fileWriter);
			zip.setLevel(9);
			addFolderToZip("", srcFolder, zip);
		} catch (final IOException e) {
			LogHelper.error("Unable to zip the directory " + srcFolder);
		} finally {
			try {
				zip.flush();
				IOUtils.closeQuietly(zip);
			} catch (final IOException e) { /**/
			}
		}
	}
	
	/**
	 * Extract a zip file to the given destination
	 */
	public static void extractZip(final File zipFile, final File destination) throws ZipException, IOException {

	    ZipFile zip = new ZipFile(zipFile);
	    Enumeration<?> zipFileEntries = zip.entries();

	    // Process each entry
	    while (zipFileEntries.hasMoreElements()) {
	        // Grab a zip file entry
	        ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
	        String currentEntry = entry.getName();
	        File destFile = new File(destination, currentEntry);
	        File destinationParent = destFile.getParentFile();

	        // Create the parent directory structure if needed
	        destinationParent.mkdirs();

	        if (!entry.isDirectory()) {
	            BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
	            // Write the current file to disk
	            FileOutputStream fos = new FileOutputStream(destFile);
	            BufferedOutputStream dest = new BufferedOutputStream(fos);
	            
	            IOUtils.copy(is, fos);
	            
	            IOUtils.closeQuietly(dest);
	            IOUtils.closeQuietly(is);
	        }
	    }
	    final String dirName = zipFile.getName().replaceAll("Backups/", "").replaceAll(".zip", "");
	    final File extractedDir = new File(dirName);
	    final File pluginsDir = new File(".");
	    LogHelper.info("Copying from directory " + extractedDir);
	    for(final File worldDir : extractedDir.listFiles()) {
	    	final File oldWorldDir = new File(pluginsDir, worldDir.getName());
	    	LogHelper.info("Deleting old world " + oldWorldDir);
	    	FileUtils.deleteDirectory(oldWorldDir);
	    	LogHelper.info("Copying world " + worldDir);
	    	FileUtils.moveDirectoryToDirectory(worldDir, pluginsDir, false);
	    }
	    boolean clean = extractedDir.delete();
	    if(!clean) {
	    	LogHelper.error("Unable to remove the extracted directory. Some worlds may not have been successfully moved...");
	    }
	    try {
	    	zip.close();
	    } catch(Exception e) {};
	} 

	static private void addFileToZip(final String path, final String srcFile,
			final ZipOutputStream zip) throws IOException {

		final File folder = new File(srcFile);
		if (folder.isDirectory()) {
			addFolderToZip(path, srcFile, zip);
		} else {
			final byte[] buf = new byte[1024];
			int len;
			FileInputStream in = null;
			try {
				in = new FileInputStream(srcFile);
				zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
				while ((len = in.read(buf)) > 0) {
					zip.write(buf, 0, len);
				}
			} finally {
				IOUtils.closeQuietly(in);
			}
		}
	}

	static private void addFolderToZip(final String path, final String srcFolder,
			final ZipOutputStream zip) throws IOException {
		final File folder = new File(srcFolder);

		for (final String fileName : folder.list()) {
			if (path.equals("")) {
				addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
			} else {
				addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
			}
		}
	}
	
	public enum BackupMode {
		SMP, BUKKIT;
	}
}
