package ch.marlovits.plz;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.List;

import org.geonames.InvalidParameterException;
import org.geonames.PostalCode;
import org.geonames.PostalCodeSearchCriteria;
import org.geonames.Toponym;
import org.geonames.ToponymSearchCriteria;
import org.geonames.ToponymSearchResult;
import org.geonames.WebService;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.org.apache.xerces.internal.parsers.DOMParser;

import ch.elexis.data.PersistentObject;
import ch.elexis.util.SWTHelper;
import ch.rgw.tools.ExHandler;
import ch.rgw.tools.JdbcLink;

enum NameTypes	{COUNTRYNAME,
				 CURRENCYNAME,
				 CITYNAME,
				 CURRENCYNAME_OTHER
				}

enum CountryCodesInputFields {	iso2,
								iso3,
								isoNum,
								fips,
								name,
								capital,
								area,
								population,
								continent,
								tld,
								currencyCode,
								currencyName,
								phone,
								postalCodeFormat,
								postalCodeRegex,
								languages,
								geonameId,
								neigbours,
								equivalentfips
								}

enum NameCodesInputFields { id,
							nameId,
							language,
							name,
							isPreferredName,
							isShortName
						   }
enum PlzInputFields {onrp,
					 plzTyp,
					 plz,
					 zusatzZiffer,
					 ort18,
					 ort27,
					 kanton,
					 sprachCode,
					 sprachCode2,
					 sortierFile,
					 briefZustellung,
					 gemeindeNr,
					 gueltigAb
					 }


public class DataImporter {
	// *** Konstanten für die Iso-Land-Daten
	private static final String COUNTRYCODES_URL = "http://download.geonames.org/export/dump/countryInfo.txt";
	private static final String COUNTRYCODES_FIELDDELIMITER = "\t";
	
	private static final String NAMES_SOURCE = "D:\\Dokumente und Einstellungen\\Harry\\Desktop\\alternateNames.txt";
	
	public DataImporter() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	// ****************************************************************************************
	// ***  Import von Länder-Codes iso2/iso3/isonum, etc  ************************************
	// ****************************************************************************************
	/**
	 * Importiert von GeoNames die aktuelle Iso-Länderliste.
	 * Falls schon Einträge vorhanden sind, werden alle als deleted markiert.
	 * Für jeden neuen Eintrag wird der alte gelöscht (Iso2 ist gleich)
	 * Auf diese Weise wird die Liste aktualisiert; wenn Länder in der Source-Liste
	 * gelöscht werden, dann verbleibt der Eintrag als gelöscht in der Datenbank.
	 * So bleiben Referenzen gültig, das Land kann aber nicht mehr neu ausgewählt werden.
	 * 
	 * Die geladene Iso-Liste enthält die offizielle aktuelle Version, allerdings in englisch
	 * Die benötigten weiteren Daten werden von geonames via Abfrage abgerufen (xml) und dann ebenfalls
	 * eingelesen
	 * http://ws.geonames.org/search?country=CH&featureCode=PCLI&lang=it
	 * 
	 * @param lang: die Sprachen, für welche die Daten aktualisiert/importiert werden sollen
	 *              (de, en, fr, it, etc.), comma-delimited. Englisch wird immer importiert.
	 */
	static public void importCountryData(final String lang)	{
		try {
			String currLanguage;
			String countryName;
			URL url = new URL(COUNTRYCODES_URL);
			URLConnection urlConnection = url.openConnection();
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
			// grösstes lastupdate einlesen - alle Einträge, die <= lastUpdate sind, werden später als
			// deleted markiert, da alle noch vorhandnen anderen aktualisiert sind mit neuerem Zeitmarker
			StringBuffer sql = new StringBuffer(300);
			sql.append("select lastupdate from " + CountryEintrag.getTableName1() + " order by lastupdate desc");
			String lastUpdate = PersistentObject.getConnection().queryString(sql.toString());
			//System.out.println(lastUpdate);
			// durch den Input gehen und alles einlesen
			String inputLine;
			ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
			ToponymSearchResult searchResult = null;
			List<Toponym> toponyms;
			NameEintrag nameEintrag;
			String countryID;
			String lLang = "," + lang + ",";
			lLang = lang.replace(" ", "");		// keine Leerschläge
			lLang = lang.toLowerCase();			// lowercase
			lLang = lLang.replace(",en,", "");	// Englisch entfernen
			String[] languageArray = lang.split(",");
			while ((inputLine = inputStream.readLine()) != null)	{
				String firstChar = inputLine.substring(0, 1);
				if (!firstChar.equals("#"))	{	// skip comment lines
					//System.out.println(inputLine);
					String[] fields = inputLine.split(COUNTRYCODES_FIELDDELIMITER);
					// den bestehenden Eintrag einlesen oder einen neuen erstellen -> country
					countryID = fields[CountryCodesInputFields.iso2.ordinal()] + "_en";
					CountryEintrag country = new CountryEintrag(countryID);
					currLanguage = "en";
					// den gefundenen/erstellten Eintrag mit den Werten aktualisieren/füllen
					country.updateEntry(fields[CountryCodesInputFields.iso2.			ordinal()],
										fields[CountryCodesInputFields.iso3.			ordinal()],
										Integer.parseInt(fields[CountryCodesInputFields.isoNum.ordinal()]),
										fields[CountryCodesInputFields.fips.			ordinal()],
										fields[CountryCodesInputFields.name.			ordinal()],
										fields[CountryCodesInputFields.tld.				ordinal()],
										fields[CountryCodesInputFields.currencyCode.	ordinal()],
										fields[CountryCodesInputFields.currencyName.	ordinal()],
										fields[CountryCodesInputFields.phone.			ordinal()],
										fields[CountryCodesInputFields.postalCodeFormat.ordinal()],
										fields[CountryCodesInputFields.postalCodeRegex.	ordinal()],
										fields[CountryCodesInputFields.languages.		ordinal()],
										Integer.parseInt(fields[CountryCodesInputFields.geonameId.ordinal()]),
										(fields.length - 1 < CountryCodesInputFields.neigbours.ordinal()) ? null : fields[CountryCodesInputFields.neigbours.ordinal()],
										currLanguage);
					// übersetzte Landesbezeichnung in ch_marlovits_name erstellen oder aktualisieren
					nameEintrag = new NameEintrag(countryID);
					nameEintrag.updateEntry(fields[CountryCodesInputFields.geonameId.ordinal()],
											currLanguage,
											fields[CountryCodesInputFields.name.ordinal()],
											"",
											"",
											Integer.toString(NameTypes.COUNTRYNAME.ordinal()));
					// übersetzte Währungsbezeichnung in ch_marlovits_name erstellen oder aktualisieren
					/*
					// ich benutze die Werte aus http://unicode.org
					nameEintrag = new NameEintrag(countryID + "_" + NameTypes.CURRENCYNAME.ordinal());
					nameEintrag.updateEntry(fields[CountryCodesInputFields.geonameId.ordinal()],
											currLanguage,
											fields[CountryCodesInputFields.currencyName.ordinal()],
											"",
											"",
											Integer.toString(NameTypes.CURRENCYNAME.ordinal()));
					*/
					// nun die gewünschten Sprach-Einträge einlesen
					// den bestehenden Eintrag einlesen oder einen neuen erstellen -> country
					for (int i = 0; i < languageArray.length;  i++)	{
						currLanguage = languageArray[i];
						countryID = fields[CountryCodesInputFields.iso2.ordinal()] + "_" + currLanguage;
						try {
							// in geoNames nach dem übersetzten Namen suchen:
							// CountryCode=<xx> -> getCountryName()
							searchCriteria.setLanguage(currLanguage);
							searchCriteria.setCountryCode(fields[CountryCodesInputFields.iso2.ordinal()]);
							searchCriteria.setMaxRows(1);
							searchResult = WebService.search(searchCriteria);
							toponyms = searchResult.getToponyms();
							if (toponyms.isEmpty())	{
								// wenn nicht gefunden, dann die englische Bezeichnung benutzen
								countryName = fields[CountryCodesInputFields.name.ordinal()];
							} else {
								countryName = toponyms.get(0).getCountryName();
							}							
							// übersetzte Landesbezeichnung in ch_marlovits_name erstellen oder aktualisieren
							String langId = fields[CountryCodesInputFields.iso2.ordinal()] + "_" + currLanguage;
							nameEintrag = new NameEintrag(langId);
							nameEintrag.updateEntry(fields[CountryCodesInputFields.geonameId.ordinal()],
													currLanguage,
													countryName,
													"",
													"",
													Integer.toString(NameTypes.COUNTRYNAME.ordinal()));
						} catch (InvalidParameterException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			for (int i = 0; i < languageArray.length;  i++)	{
				currLanguage = languageArray[i];
				// für die aktuelle Sprache alle Währungsbezeichnungen in ch_marlovits_names einlesen
				String[][] languagesArray = getLanguagesTable(currLanguage);
				String langId;
				for (int langIx = 0; langIx < languagesArray.length; langIx++)	{
					langId = languagesArray[langIx][0] + "_" + currLanguage + "_" + NameTypes.CURRENCYNAME.ordinal();
					nameEintrag = new NameEintrag(langId);
					nameEintrag.updateEntry(languagesArray[langIx][0],
											currLanguage,
											languagesArray[langIx][1],
											"",
											"",
											Integer.toString(NameTypes.CURRENCYNAME.ordinal()));
					langId = languagesArray[langIx][0] + "_" + currLanguage + "_" + NameTypes.CURRENCYNAME_OTHER.ordinal();
					nameEintrag = new NameEintrag(langId);
					nameEintrag.updateEntry(languagesArray[langIx][0],
											currLanguage,
											languagesArray[langIx][2],
											"",
											"",
											Integer.toString(NameTypes.CURRENCYNAME_OTHER.ordinal()));
				}
			}
			// alle Einträge mit lastupdate <= lastUpdate als deleted markieren -
			// sind nicht mehr inder Liste vorhanden, ergo ab sofort ungültig
			// TODO noch nicht sauber
			sql = new StringBuffer(300);
			sql.append("UPDATE CH_MARLOVITS_COUNTRY set deleted = " + JdbcLink.wrap("1") + "where lastupdate <= " + lastUpdate + " and (entrylanguage != " + JdbcLink.wrap("en") + " and entrylanguage != " + JdbcLink.wrap(lang) + ")");
			PersistentObject.getConnection().exec(sql.toString());
			// input schliessen
			inputStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			SWTHelper.alert("Fehler während des Importes.", "Fehler während des Importes");
			e.printStackTrace();
		}
 	}

	// ****************************************************************************************
	// ***  Import von allen AlternateNames aus GeoNames   ************************************
	// ***  VORSICHT: riesige Datenmenge!                  ************************************
	// ****************************************************************************************
	static public void importNameData()	{
  	  	//new NameEintrag("0", "0", "", "", "", "");
	      try {
	          FileReader fr = new FileReader(NAMES_SOURCE);
	          BufferedReader br = new BufferedReader(fr);
	          int cnt = 0;
	          String fieldsLine;
        	  StringBuffer sql = new StringBuffer(300);
        	  String[] fieldsArray;
        	  NameEintrag nameEintrag;
        	  JdbcLink j = PersistentObject.getConnection();
	          while ((fieldsLine = br.readLine()) != null)	{
	      		  long ts = System.currentTimeMillis();
	        	  fieldsArray = fieldsLine.split("	");
	        	  sql.setLength(0);
	        	  sql.append("select count(*) as cnt from CH_MARLOVITS_NAME_GEONAMES where id = " + JdbcLink.wrap(fieldsArray[NameCodesInputFields.id.ordinal()]) + ")");
	        	  int count = PersistentObject.getConnection().queryInt(sql.toString());
	        	  if (count == 0)	{
	        		  // Direktaufruf von SLQ, da Statement unten schnell Fehler ergibt bei riesiger Datei
	        		  // gibt wie unten stehender Code irgendwann einen Heap out of mem error, aber vieeeeel später
		        	  sql.setLength(0);
		        	  sql.append("insert into CH_MARLOVITS_NAME_GEONAMES ( id, nameid, \"language\", name, ispreferredname, isshortname, deleted, lastupdate) values (");
		        	  sql.append(JdbcLink.wrap(fieldsArray[NameCodesInputFields.id.      ordinal()]) + ", ");
		        	  sql.append(JdbcLink.wrap(fieldsArray[NameCodesInputFields.nameId.  ordinal()]) + ", ");
		        	  sql.append(JdbcLink.wrap(fieldsArray[NameCodesInputFields.language.ordinal()]) + ", ");
		        	  sql.append(JdbcLink.wrap((fieldsArray.length - 1 < NameCodesInputFields.name.           ordinal()) ? null : fieldsArray[NameCodesInputFields.name.           ordinal()]) + ", ");
		        	  sql.append(JdbcLink.wrap((fieldsArray.length - 1 < NameCodesInputFields.isPreferredName.ordinal()) ? null : fieldsArray[NameCodesInputFields.isPreferredName.ordinal()]) + ", ");
		        	  sql.append(JdbcLink.wrap((fieldsArray.length - 1 < NameCodesInputFields.isShortName.    ordinal()) ? null : fieldsArray[NameCodesInputFields.isShortName.    ordinal()]) + ", ");
		        	  sql.append(JdbcLink.wrap("0") + ",");
		        	  sql.append(ts);
		        	  sql.append(")");
	        		  j.exec(sql.toString());
	        		  /* der folgende Code geht nicht - gibt einen Heap out of memory error, ist nicht wegzukriegen...
	        		  nameEintrag = new NameEintrag(fieldsArray[NameCodesInputFields.id.			ordinal()],
									  				fieldsArray[NameCodesInputFields.nameId.		ordinal()],
									  				fieldsArray[NameCodesInputFields.language.		ordinal()],
									  				(fieldsArray.length - 1 < NameCodesInputFields.name.ordinal()) ? null : fieldsArray[NameCodesInputFields.name.ordinal()],
									  				(fieldsArray.length - 1 < NameCodesInputFields.isPreferredName.ordinal()) ? null : fieldsArray[NameCodesInputFields.isPreferredName.ordinal()],
									  				(fieldsArray.length - 1 < NameCodesInputFields.isShortName.ordinal()) ? null : fieldsArray[NameCodesInputFields.isShortName.ordinal()]);
					  */
	       	  }
	        	  System.out.println(cnt);
	        	  cnt++;
	          }
	          br.close();
	          System.out.println(cnt);
	        }
	        catch (IOException e) {
	          System.err.println(e);
	        }
 	}
	
	/**
	 * Liest Inhalt einer ganzen Seite einer übergebenen URL
	 * @param url: die einzulesende URL
	 * @return die ganze HTML-Seite als String
	 */
	public static String readHTMLPage(final String url) throws IOException, MalformedURLException{
		URL content = new URL(url);
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
	
	// ****************************************************************************************
	// ***  GeoNames Stuff                                 ************************************
	// ****************************************************************************************
	public static void geoNamesTest()	{
		try {
			ToponymSearchCriteria searchCriteria = new ToponymSearchCriteria();
			PostalCodeSearchCriteria postalCodeSearchCriteria = new PostalCodeSearchCriteria();
			postalCodeSearchCriteria.setCountryCode("CH");
			//postalCodeSearchCriteria.setPostalCode("8307");
			postalCodeSearchCriteria.setPlaceName("Chur");
			List<PostalCode> postalCodes = WebService.postalCodeSearch(postalCodeSearchCriteria);
			ToponymSearchResult searchResult = null;
			for (PostalCode postalCode : postalCodes){
				String thePlaceName = postalCode.getPlaceName();
				System.out.println("*****************************");
				System.out.println(thePlaceName + " " + postalCode.getAdminCode1());
				searchCriteria.setNameEquals(thePlaceName);
				searchCriteria.setLanguage("it");
				searchCriteria.setCountryCode("CH");
				searchResult = WebService.search(searchCriteria);
				for (Toponym toponym : searchResult.getToponyms()) {
					System.out.println(toponym.getName()+" "+ toponym.getCountryName());
				}	
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
/*
		ToponymSearchCriteria searchCriteria;
		searchCriteria = new ToponymSearchCriteria();
		searchCriteria.setQ("zurich");
		ToponymSearchResult searchResult = null;
		try {
			searchResult = WebService.search(searchCriteria);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Toponym toponym : searchResult.getToponyms()) {
			System.out.println(toponym.getName()+" "+ toponym.getCountryName());
		}	
*/	}
	
	// ****************************************************************************************
	// ***  PLZ Schweiz                                    ************************************
	// ****************************************************************************************
	// https://match.postmail.ch/match_zip?SIT_ID=5&SPRCDE=1
	// https://match.postmail.ch/archive_plz
	public static void importTabDelimited(){
				try {
			String fileName = "D:\\Dokumente und Einstellungen\\Harry\\Desktop\\DatenFürElexis\\plzs\\CH_Post\\plz_p1_20090325.txt";
	        FileInputStream fis = new FileInputStream(fileName); 
	        InputStreamReader isr = new InputStreamReader(fis, "ISO-8859-1"); 
	        BufferedReader br = new BufferedReader(isr);
	        String delimiter = "	";
			String line;
			while ((line = br.readLine()) != null) {
				String tmp = URLDecoder.decode(line, "ISO-8859-1");
				byte[] bytes = line.getBytes();
				
				String[] fieldsArray = line.split(delimiter);
				new PlzEintrag(	fieldsArray[PlzInputFields.onrp.			ordinal()],
								fieldsArray[PlzInputFields.plzTyp.			ordinal()],
								fieldsArray[PlzInputFields.plz.				ordinal()],
								fieldsArray[PlzInputFields.zusatzZiffer.	ordinal()],
								fieldsArray[PlzInputFields.ort18.			ordinal()],
								fieldsArray[PlzInputFields.ort27.			ordinal()],
								fieldsArray[PlzInputFields.kanton.			ordinal()],
								fieldsArray[PlzInputFields.sprachCode.		ordinal()],
								fieldsArray[PlzInputFields.sprachCode2.		ordinal()],
								fieldsArray[PlzInputFields.sortierFile.		ordinal()],
								fieldsArray[PlzInputFields.briefZustellung.	ordinal()],
								fieldsArray[PlzInputFields.gemeindeNr.		ordinal()],
								fieldsArray[PlzInputFields.gueltigAb.		ordinal()],
								"de",
								"",
								"ch");
			}
			return;
		} catch (Exception ex) {
			ExHandler.handle(ex);
			return;
		}
		
	}

	public static String[][] getLanguagesTable(final String language)	{
		String urlText= "http://unicode.org/cldr/data/common/main/" + language + ".xml";
		String[][] currenciesArray = null;
		
		// den Parser anzapfen
		DOMParser p = new DOMParser();
		// den Inhalt einlesen
		try {
			p.parse(urlText);
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Document doc = p.getDocument();
		
		// next
		
		int currencyIndex = 0;
		Node n = doc.getDocumentElement().getFirstChild();
		n = n.getNextSibling();
		NodeList nList = n.getParentNode().getChildNodes();
		for (int i = 0; i < nList.getLength(); i++)	{
			Node currN = nList.item(i);
			String currNodeName = currN.getNodeName();
			if (currNodeName.equals("numbers"))	{
				NodeList currenciesNodesList = currN.getChildNodes();
				int numOfNodes = currenciesNodesList.getLength();
				for (int j = 0; j < numOfNodes; j++)	{
					// loop in <numbers>, bis 
					Node currNode2 = currenciesNodesList.item(j);
					if (currNode2.getNodeName().equals("currencies"))	{
						// currencyNodeList contains the currency Nodes
						NodeList currencyNodeList = currNode2.getChildNodes();
						currenciesArray = new String[currencyNodeList.getLength()][3];
						for (int k = 0; k < currencyNodeList.getLength(); k++)	{
							// currNode3 contains one currency Node
							Node currNode3 = currencyNodeList.item(k);
							if (currNode3.getNodeName().equals("currency"))	{
								String theType = "";
								// currency subNodes
								NodeList currencySubNodeList = currNode3.getChildNodes();
								if (currNode3.hasAttributes())	{
									NamedNodeMap nnm = currNode3.getAttributes();
									theType = nnm.getNamedItem("type").getNodeValue();
									if (theType.equals("")) break;
								} else	{
									break;		// ohne Währungs-Iso gibt es keinen Eintrag -> skip
								}
								// Währungsbezeichnungen extrahieren: ohne attributes = Einzahl, count="other": Mehrzahl
								String singular = "";
								String plural   = "";
								for (int l = 0; l < currencySubNodeList.getLength(); l++)	{
									Node currNode4 = currencySubNodeList.item(l);
									Node tmpNode;
									if ((currNode4.getNodeType() != 3) && (currNode4.getNodeName() == "displayName"))	{
										NamedNodeMap nnm = currNode4.getAttributes();
										String countVal = ((tmpNode = nnm.getNamedItem("count")) == null) ? "" : tmpNode.getNodeValue();
										String altVal   = ((tmpNode = nnm.getNamedItem("alt"))   == null) ? "" : tmpNode.getNodeValue();
										String draftVal = ((tmpNode = nnm.getNamedItem("draft")) == null) ? "" : tmpNode.getNodeValue();
										if ((countVal.equals("")) && (altVal.equals("")) && (draftVal.equals("")))	{
											singular = currNode4.getTextContent();
										}
										if ((countVal.equals("other")) && (altVal.equals("")) && (draftVal.equals("")))	{
											plural = currNode4.getTextContent();
										}
									}
								}
								if (plural.equals("")) plural = singular;
								currenciesArray[currencyIndex][0] = theType;
								currenciesArray[currencyIndex][1] = singular;
								currenciesArray[currencyIndex][2] = plural;
								currencyIndex++;
							}
						}
					}
				}
				break;
			}
		}
	String[][] smallArray = new String[currencyIndex][3];
	System.arraycopy(currenciesArray, 0, smallArray, 0, currencyIndex);
	return smallArray;
	}
}