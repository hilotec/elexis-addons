/**
 * 
 */
package at.herzpraxis.elexis.connect.cobasmira;

import gnu.io.SerialPortEvent;

import java.io.IOException;
import java.io.InputStream;

import ch.elexis.rs232.AbstractConnection;
import ch.elexis.util.Log;

/**
 * @author Marco Descher / Herzpraxis Dr. Wolber, Goetzis, Austria
 * 
 */
public class CobasMiraConnection extends AbstractConnection {
	Log _elexislog = Log.get("CobasMiraConnection");
	private static StringBuffer textBuf = new StringBuffer();
	private static final int SOH = 0x01;   	// Start of Heading
	private static final int STX = 0x02;	// Start of Text within a Cobas Mira Measurement
	private static final int ETX = 0x03;	// End of Text within a Cobas Mira Measurement (see sampledata/cobaslogsingle.txt for info)
	private static final int EOT = 0x04;	// End of Transmission

	/**
	 * @param portName
	 * @param port
	 * @param settings
	 * @param l
	 */
	public CobasMiraConnection(String portName, String port, String settings,
			ComPortListener l) {
		super(portName, port, settings, l);
	}

	/*
	 * Only Data in between a STXLF and ETXLF is captured and forwarded to the Listeners
	 * see sampledata/cobaslogsingle.txt for information about the message structure
	 * 
	 * @see ch.elexis.rs232.AbstractConnection#serialEvent(int,
	 * java.io.InputStream, gnu.io.SerialPortEvent)
	 */
	@Override
	public void serialEvent(int state, InputStream inputStream,
			SerialPortEvent e) throws IOException {
		int data = inputStream.read();
		if (data == STX) {
			_elexislog.log("Start of stream: " + data +  " (STX)", Log.DEBUGMSG);
			textBuf = new StringBuffer();
			data = inputStream.read();
		} else {
			_elexislog.log("Continue stream..", Log.DEBUGMSG);
		}
		
		while ((data != -1) && (data != ETX)) {
			textBuf.append((char) data);
			data = inputStream.read();
		}
		// Log output
		String text = "";
		if (data == -1) {
			text = " (EOF)";
		}
		if (data == ETX) {
			text = " (ETX)";
		}
		_elexislog.log("End of stream: " + data + text, Log.DEBUGMSG);
		
		if (data == ETX) {
			_elexislog.log("buffer: " + textBuf.toString(), Log.DEBUGMSG);
			this.listener.gotData(this, textBuf.toString().getBytes());	// Handle the received data
			textBuf = new StringBuffer();
		}
	}

}
