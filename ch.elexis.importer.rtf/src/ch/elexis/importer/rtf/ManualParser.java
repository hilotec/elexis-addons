package ch.elexis.importer.rtf;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.elexis.StringConstants;
import ch.elexis.data.Patient;
import ch.elexis.exchange.KontaktMatcher;
import ch.elexis.exchange.KontaktMatcher.CreateMode;
import ch.elexis.util.Log;

public class ManualParser {
	final String remove = "(\\\\[a-z0-9]+|\\\\\\*|[\\{\\}])";
	final String marker = "Betrifft:\\s+";
	final Log log = Log.get("RTF Parser");
	String name = "PA_Name/Vorname\\s+([^,]+),\\s*";
	String street = "PA_Strasse\\s+([^,]+),\\s*";
	String place = "PA_Ortschaft\\s+([^,]+),\\s*";
	String birthdate = "PA_Geburtsdatum\\s+([0-9][0-9]\\.[0-9][0-9]\\.[0-9]{2,4})";
	String unUTF8 = "\\\\u[0-9]+;";

	Pattern personalia = Pattern.compile(marker + name + street + place
			+ birthdate, Pattern.MULTILINE);
	Pattern personalia2 = Pattern
			.compile("Betrifft:\\s*\\\\cell\\s*([^,]+),\\s*([^,]+)\\s*([^,]+)}");

	public Patient findPatient(File in) throws Exception {
		FileInputStream fis = new FileInputStream(in);
		InputStreamReader ir = new InputStreamReader(fis, "ISO-8859-1");
		int len = (int) in.length();
		char[] buffer = new char[5000];
		ir.read(buffer);
		String s = new String(buffer).replaceAll(remove, "");
		Matcher matcher = personalia.matcher(s);

		if (matcher.find()) {
			String lastname = matcher.group(1).trim();
			String pStreet = matcher.group(2).trim();
			String pDOB = matcher.group(4);
			String firstname = "";
			String[] pPlace = KontaktMatcher.normalizeAddress(matcher.group(3)
					.trim());
			String[] pers = lastname.split(" +", 2);
			if (pers.length > 1) {
				lastname = pers[0];
				firstname = pers[1];
			}
			lastname = lastname.replaceAll(unUTF8, "%");
			firstname = firstname.replaceAll(unUTF8, "%");
			Patient pat = KontaktMatcher.findPatient(lastname, firstname, pDOB,
					StringConstants.EMPTY, pStreet.replaceAll(unUTF8, "%"),
					pPlace[0], pPlace[1].replaceAll(unUTF8, "%"),
					StringConstants.EMPTY, CreateMode.FAIL);
			if (pat == null) {
				log.log("Did not find patient matching " + lastname + " "
						+ firstname + " " + pDOB + " in "
						+ in.getAbsolutePath(), Log.ERRORS);
			}
			return pat;
		} else {
			matcher = personalia2.matcher(s);
			if (matcher.find()) {
				String lastname = matcher.group(1).trim();
				String pStreet = matcher.group(2).trim();
				String firstname = "";
				String[] pPlace = KontaktMatcher.normalizeAddress(matcher.group(3)
						.trim());
				String[] pers = lastname.split(" +", 2);
				if (pers.length > 1) {
					lastname = pers[0];
					firstname = pers[1];
				}
				lastname = lastname.replaceAll(unUTF8, "%");
				firstname = firstname.replaceAll(unUTF8, "%");
				Patient pat = KontaktMatcher.findPatient(lastname, firstname, null,
						StringConstants.EMPTY, pStreet.replaceAll(unUTF8, "%"),
						pPlace[0], pPlace[1].replaceAll(unUTF8, "%"),
						StringConstants.EMPTY, CreateMode.FAIL);
				if (pat == null) {
					log.log("Did not find patient matching " + lastname + " "
							+ firstname + " in "
							+ in.getAbsolutePath(), Log.ERRORS);
				}
				return pat;
			} else {
				log.log("Did not find Patient identifier in "
						+ in.getAbsolutePath(), Log.ERRORS);
			}
		}

		return null;
	}
}
