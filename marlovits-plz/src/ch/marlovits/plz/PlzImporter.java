package ch.marlovits.plz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;

import javax.net.SocketFactory;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V1CertificateGenerator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import ch.elexis.util.ImporterPage;

public class PlzImporter extends ImporterPage {

	@Override
	public Composite createPage(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(2,false));
		new Label(comp, SWT.NONE).setText("Test");
		Button btn = new Button(comp, SWT.NONE);
		btn.setText("Import CH");
		btn.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(final SelectionEvent e){
				importCH();
			}
		});
		return comp;
	}

	@Override
	public IStatus doImport(IProgressMonitor monitor) throws Exception {
		// TODO Auto-generated method stub
		//return null;
		return Status.OK_STATUS;
	}

	@Override
	public String getDescription() {
		return "Import der Schweizer Postleitzahlen 2";
	}

	@Override
	public String getTitle() {
		return "Schweizer Postleitzahlen";
	}
	private void importCH()	{
		//URL url = new URL("https://match.postmail.ch/download?id=10001&tid=11");
		//readContent("");
		test();
	}
	
/**
 * Liest Inhalt einer Web-Abfrage
 */
public static String readContent(final String url)
	throws IOException, MalformedURLException{
	URL content = new URL("https://match.postmail.ch/download?id=10001&tid=11");
	InputStream input = content.openStream();
	
	StringBuffer sb = new StringBuffer();
	int count = 0;
	char[] c = new char[10000];
	InputStreamReader isr = new InputStreamReader(input);
	try {
		while ((count = isr.read(c)) > 0) {
			sb.append(c, 0, count);
		}
	} finally {
		if (input != null) {
			input.close();
		}
	}
	return sb.toString();
}

public static void saveBinaryFile() {
    int bufferLength = 128;
    try {
      URL u = new URL("https://match.postmail.ch/download?id=10001&tid=11");
      URLConnection uc = u.openConnection();
      String ct = uc.getContentType();
      int contentLength = uc.getContentLength();
      if (ct.startsWith("text/") || contentLength == -1) {
        System.err.println("This is not a binary file.");
        return;
      }
      
      InputStream stream = uc.getInputStream();
      byte[] buffer = new byte[contentLength];
      int bytesread = 0;
      int offset = 0;
      while (bytesread >= 0) {
        bytesread = stream.read(buffer, offset, bufferLength);
        if (bytesread == -1)
          break;
        offset += bytesread;
      }
      if (offset != contentLength) {
        System.err.println("Error: Only read " + offset + " bytes");
        System.err.println("Expected " + contentLength + " bytes");
      }
      
      String theFile = u.getFile();
      theFile = theFile.substring(theFile.lastIndexOf('/') + 1);
      FileOutputStream fout = new FileOutputStream(theFile);
      fout.write(buffer);
    } catch (Exception e) {
      System.err.println(e);
    }
    return;
  }

public void test4()	{
	 URL url;
	try {
	    FileInputStream is = new FileInputStream("your.keystore");

	    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
	    keystore.load(is, "my-keystore-password".toCharArray());

	    String alias = "myalias";
	    Certificate cert = keystore.getCertificate(alias);

	    CertificateFactory certFact = CertificateFactory.getInstance("X.509");
	    CertPath path = certFact.generateCertPath(Arrays.asList(new Certificate[]{cert}));

	  
	    url = new URL("https://match.postmail.ch/download?id=10001&tid=11");
	     HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
	     KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType()); 
	     InputStream trustStore = null;
		char[] trustStorePassword = null;
			keyStore.load(trustStore, trustStorePassword);
		     trustStore.close(); 
		     TrustManagerFactory tmf =  
		       TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()); 
		     tmf.init(keyStore); 
		     SSLContext ctx = SSLContext.getInstance("TLS"); 
		     ctx.init(null, tmf.getTrustManagers(), null); 
		     SSLSocketFactory sslFactory = ctx.getSocketFactory();
				connection.setSSLSocketFactory(sslFactory); 
		     connection.setDoOutput(true);
		     BufferedReader in = new BufferedReader(
		               new InputStreamReader(connection.getInputStream()));
		     String line;
		     while ((line = in.readLine()) != null) {
		        System.out.println(line);
		     }
		     in.close();
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (KeyStoreException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (NoSuchAlgorithmException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (CertificateException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (KeyManagementException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public void test3()	{
	int HTTPS_PORT = 443;
	// Get a Socket factory 
    SocketFactory factory = SSLSocketFactory.getDefault(); 

    // Get Socket from factory 
    Socket socket;
	try {
		socket = factory.createSocket("match.postmail.ch/download?id=10001&tid=11", HTTPS_PORT);
		//socket = factory.createSocket("www.stadtpraxis.ch", HTTPS_PORT);
	    BufferedWriter out;
		out = new BufferedWriter(new 
				OutputStreamWriter(socket.getOutputStream()));
	    BufferedReader in = new BufferedReader(
	    		new InputStreamReader(socket.getInputStream()));
	        out.write("GET / HTTP/1.0\n\n");
	        out.flush();

	        String line;
	        StringBuffer sb = new StringBuffer();
	        while((line = in.readLine()) != null) {
	           sb.append(line);
	        }
	        out.close();
	        in.close();
	        System.out.println(sb.toString());
	} catch (UnknownHostException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	} 
     
}

public void test2() {
    URL yahoo;
	try {
		//yahoo = new URL("https://match.postmail.ch/download?id=10001&tid=11");
		yahoo = new URL("http://www.stadtpraxis.ch");
	    URLConnection yc = yahoo.openConnection();
	    BufferedReader in = new BufferedReader(
	                            new InputStreamReader(
	                            yc.getInputStream()));
	    String inputLine;

	    while ((inputLine = in.readLine()) != null) 
	        System.out.println(inputLine);
	    in.close();
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public void test5()	{
	try {
		KeyPair pair = generateRSAKeyPair();
	    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	    ByteArrayOutputStream bOut = new ByteArrayOutputStream();
	    bOut.write(generateV1Certificate(pair).getEncoded());
	    bOut.close();
	    InputStream in = new ByteArrayInputStream(bOut.toByteArray());
	    CertificateFactory fact = CertificateFactory.getInstance("X.509", "BC");
	    X509Certificate x509Cert = (X509Certificate) fact.generateCertificate(in);
	    System.out.println("issuer: " + x509Cert.getIssuerX500Principal());
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
}

public static X509Certificate generateV1Certificate(KeyPair pair) throws InvalidKeyException,
	NoSuchProviderException, SignatureException {
	//Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
	Security.addProvider(new BouncyCastleProvider());
	X509V1CertificateGenerator certGen = new X509V1CertificateGenerator();
	
	certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
	certGen.setIssuerDN(new X500Principal("CN=Test Certificate"));
	certGen.setNotBefore(new Date(System.currentTimeMillis() - 10000));
	certGen.setNotAfter(new Date(System.currentTimeMillis() + 10000));
	certGen.setSubjectDN(new X500Principal("CN=Test Certificate"));
	certGen.setPublicKey(pair.getPublic());
	certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");

	return certGen.generateX509Certificate(pair.getPrivate(), "BC");
}

public static KeyPair generateRSAKeyPair() throws Exception {
	KeyPairGenerator kpGen = KeyPairGenerator.getInstance("RSA", "BC");
	kpGen.initialize(1024, new SecureRandom());
	return kpGen.generateKeyPair();
}

public void test()	{

	    //if (args.length == 0) {
	    //  System.out.println("Usage: java HTTPSClient2 host");
	    //  return;
	    //}

	    int port = 443; // default https port
	    String host = "match.postmail.ch/download?id=10001&tid=11";
	    host = "www.stadtpraxis.ch";
	    host = "www.postmail.ch";

	    try {
	      SSLSocketFactory factory
	       = (SSLSocketFactory) SSLSocketFactory.getDefault();

	      SSLSocket socket = (SSLSocket) factory.createSocket(host, port);

	      // enable all the suites
	      String[] supported = socket.getSupportedCipherSuites();
	      socket.setEnabledCipherSuites(supported);

	      Writer out = new OutputStreamWriter(socket.getOutputStream());
	      // https requires the full URL in the GET line
	      out.write("GET http://" + host + "/ HTTP/1.1\r\n");
	      out.write("Host: " + host + "\r\n");
	      out.write("\r\n");
	      out.flush();

	      // read response
	      BufferedReader in = new SafeBufferedReader(
	        new InputStreamReader(socket.getInputStream()));

	      // read the header
	      String s;
	      while (!(s = in.readLine()).equals("")) {
	          System.out.println(s);
	      }
	      System.out.println();

	      // read the length
	      String contentLength = in.readLine();
//	      int length = Integer.MAX_VALUE;
//	      try {
//	        length = Integer.parseInt(contentLength.trim(), 16);
//	      }
//	      catch (NumberFormatException ex) {
//	        // This server doesn't send the content-length
//	        // in the first line of the response body
//	      }
	      System.out.println(contentLength);

	      int length = 1000;
	      
	      int c;
	      int i = 0;
	      while ((c = in.read()) != -1 && i++ < length) {
	        System.out.write(c);
	      }

	      System.out.println();
	      out.close();
	      in.close();
	      socket.close();

	    }
	    catch (IOException ex) {
	      System.err.println(ex);
	    }

	  }

public class SafeBufferedReader extends BufferedReader {

	  public SafeBufferedReader(Reader in) {
	    this(in, 1024);
	  }

	  public SafeBufferedReader(Reader in, int bufferSize) {
	    super(in, bufferSize);
	  }

	  private boolean lookingForLineFeed = false;
	  
	  public String readLine() throws IOException {
	    StringBuffer sb = new StringBuffer("");
	    while (true) {
	      int c = this.read();
	      if (c == -1) { // end of stream
	        if (sb.length() == 0) return null;
	        return sb.toString();
	      }
	      else if (c == '\n') {
	        if (lookingForLineFeed) {
	          lookingForLineFeed = false;
	          continue;
	        }
	        else {
	          return sb.toString();
	        }
	      }
	      else if (c == '\r') {
	        lookingForLineFeed = true;
	        return sb.toString();
	      }
	      else {
	        lookingForLineFeed = false;
	        sb.append((char) c);
	      }
	    }
	  }

	}

}


