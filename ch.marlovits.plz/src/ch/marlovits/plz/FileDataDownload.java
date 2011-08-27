package ch.marlovits.plz;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * This Java program lets you download files from one or more URLs and save them in the directory
 * where you want. This program takes destination directory for the files to save as first command
 * line argument and URLs for the files as next command line arguments separated by space. Java
 * provides URLConnection class that represents a communication link between the application and a
 * URL. Invoking the openConnection method on a URL creates URLConnection object. Now get
 * InputStream object from that connection and read the data. Finally write the data to the local
 * file.
 * 
 * @author http://www.javajazzup.com
 * 
 */
public class FileDataDownload {
	final static int size = 1024;
	
	public static void FileDownload(String fileAddress, String localFileName, String destinationDir){
		OutputStream os = null;
		URLConnection URLConn = null;
		
		// URLConnection class represents a communication link between the application and a URL.
		InputStream is = null;
		try {
			URL fileUrl;
			byte[] buf;
			int ByteRead, ByteWritten = 0;
			fileUrl = new URL(fileAddress);
			os =
				new BufferedOutputStream(
					new FileOutputStream(destinationDir + "\\" + localFileName));
			// The URLConnection object is created by invoking the openConnection method on a URL.
			URLConn = fileUrl.openConnection();
			is = URLConn.getInputStream();
			buf = new byte[size];
			while ((ByteRead = is.read(buf)) != -1) {
				os.write(buf, 0, ByteRead);
				ByteWritten += ByteRead;
			}
			System.out.println("Downloaded Successfully.");
			System.out.println("File name:" + localFileName + "\nNo of bytes :" + ByteWritten);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void fileDownload(String fileAddress, String destinationDir){
		// Find the index of last occurance of character ‘/’ and ‘.’.
		int lastIndexOfSlash = fileAddress.lastIndexOf("/");
		int lastIndexOfPeriod = fileAddress.lastIndexOf(".");
		// Find the name of file to be downloaded from the address.
		String fileName = fileAddress.substring(lastIndexOfSlash + 1);
		// Check whether path or file name is given correctly.
		if ((lastIndexOfPeriod >= 1) && (lastIndexOfSlash >= 0)
			&& (lastIndexOfSlash < fileAddress.length())) {
			FileDownload(fileAddress, fileName, destinationDir);
		} else {
			System.err.println("Specify correct path or file name.");
		}
	}
	
	public static void main(String[] args){
		// Check whether there are atleast two arguments.
		if (args.length == 2) {
			for (int i = 1; i < args.length; i++) {
				fileDownload(args[i], args[0]);
			}
		} else {
			System.err
				.println("Provide Destination directory path and file names separated by space.");
		}
	}
}
