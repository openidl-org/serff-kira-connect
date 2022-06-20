package serffproc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.*;
import org.json.simple.parser.*;

//Must use 1.8 Java runtime
public class serffjob {

	static String _username = "";
	static String _password = "";
	static String _attachmentEndpoint = "";
	static String _filingEndpoint = "";
	static String _correspondenceEndpoint = "";
	static String _PDFEndpoint = "";
	static String _fileFolder = "";
	static String _KIRAFile = "";
	public static void main(String[] args) {
		try {

			Path currentRelativePath = Paths.get("");
			String s = currentRelativePath.toAbsolutePath().toString();
			String seperator = System.getProperty("file.separator");
			File configFile = new File(s + seperator + "config.json");
			if (!configFile.exists()) {
				System.out.println("Configuration File Not Found!");
				System.exit(1);
			}

			FileReader fr = new FileReader(s + seperator + "config.json");
			JSONParser parser = new JSONParser();
			Object obj = parser.parse(fr);
			fr.close();
			Map map = (Map) obj;
			if ((Boolean) map.get("isTesting")) {
				_username = (String) map.get("serffUserNameBeta");
				_password = (String) map.get("serffPasswordBeta");
				_attachmentEndpoint = (String) map.get("serffAttachmentEndpointBeta");
				_filingEndpoint = (String) map.get("serffFilingEndpointBeta");
				_correspondenceEndpoint = (String) map.get("serffCorrespondenceEndpointBeta");
				_PDFEndpoint = (String) map.get("serffPDFEndpointBeta");
			} else {
				_username = (String) map.get("serffUserNameProd");
				_password = (String) map.get("serffPasswordProd");
				_attachmentEndpoint = (String) map.get("serffAttachmentEndpointProd");
				_filingEndpoint = (String) map.get("serffFilingEndpointProd");
				_correspondenceEndpoint = (String) map.get("serffCorrespondenceEndpointProd");
				_PDFEndpoint = (String) map.get("serffPDFEndpointProd");
			}

			_fileFolder = s + seperator + "data";
			_KIRAFile = s + seperator + "data" + seperator + "kira.json";
			utility.setServers(_filingEndpoint, _PDFEndpoint, _correspondenceEndpoint, _attachmentEndpoint);


			ArrayList<String> serffs = utility.getReadyForKiraPCFilings(_username, _password);
			for (int i = 0; i < serffs.size(); i++) {
				String serffNoToi = serffs.get(i);
				int p = serffNoToi.lastIndexOf("-");
				String serffNo =serffNoToi.substring(0,p); 
				String fileName = downloadFile(serffNoToi);
				System.out.println("Downloaded File  " + fileName);
				String r = utility.setStateStatus(_username, _password, serffNo, utility.WAITFORKIRA);
				System.out.println("Set Serff State Status:  " + r);
			}

			File file = new File(_KIRAFile);
			int nProcessed = 0;
			if (file.exists()) {
				fr = new FileReader(_KIRAFile);
				obj = parser.parse(fr);
				fr.close();
				JSONArray objs = (JSONArray) obj;
				int isFailed = 0;
				if (objs.size() > 0) {
					Iterator<JSONObject> iterator = objs.iterator();
					while (iterator.hasNext()) {
						JSONObject jo = iterator.next();
						String serff = (String) jo.get("serff");
						String details = (String) jo.get("details");
						if (details.length() > 0) {
							// Create objection letter
							String oid = utility.submitObjectionLetter(_username, _password, serff, "Kira Draft",
									new java.util.GregorianCalendar(), details, " ", new java.util.GregorianCalendar(),
									" ");
							System.out.println("Created Objection Letter for " + serff);
						}
						// Set SERFF state status to utility.REVIEWEDBYKIRA
						if (utility.setStateStatus(_username, _password, serff, utility.REVIEWEDBYKIRA) == null) {
							isFailed = 1;
							break;
						}
						nProcessed++;
					}
				}
				// Delete json file
				if (isFailed == 0)
					file.delete();
				else {
					System.out.println("Failed to Assign State Status");
				}
				Thread.sleep(5000);
				System.out.println("Delete JSON file");
			} else {
			}
			System.out.println("Downloaded " + serffs.size() + " Processed " + nProcessed);
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}


	static String downloadFile(String serffNoToi) throws Exception {
		int p = serffNoToi.lastIndexOf("-");
		String serffNo =serffNoToi.substring(0,p); 
		String ret = _fileFolder + "\\Serff " + serffNoToi + ".pdf";
		File fileDes = new File(ret);
		if (fileDes.exists())
			return ret;
		String tempFileStr = utility.downloadFilingAsPDF(_username, _password, serffNo, _fileFolder);
		if (tempFileStr != null) {
			// wait for file settle
			Thread.sleep(500);
			File tempFile = new File(_fileFolder + "\\" + tempFileStr);

			if (tempFile.exists()) {
				tempFile.renameTo(fileDes);
			} else
				throw new Exception("Failed to get document from SERFF");
		} else
			throw new Exception("Failed to get document from SERFF");
		return ret;

	}

}
