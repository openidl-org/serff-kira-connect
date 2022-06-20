package serffproc;

import java.util.*;
import javax.xml.rpc.*;

import org.naic.serff.stateapi.service.*;

public class utility {

	
	static String _filing_server = "";
	static String _pdf_server = "";
	static String _correspondence_server = "";
	static String _attachment_server = "";
	static final String READYFORKIRA = "Ready for KIRA";
	static final String WAITFORKIRA = "Wait for KIRA";
	static final String REVIEWEDBYKIRA = "Reviewed by KIRA";

	public static String[] _aLHFilingFields = { "FilingSERFFTrackNum",
			"StateTrackNum", "SubTOIName", "TOIName", "PrimaryReviewer" };

	public static String[] _aLHFormFields = { "FilingSERFFTrackNum", "FormName" // FCP_FORM
			, "FormNumber" // FCP_COMPANY_FORM
			, "FormReviewStatus" // FCP_DISPOSITION
	};

	public static String[] _aPCFormFields = { "FilingSERFFTrackNum", "FormName" // FCP_FORM
			, "FormNumber" // FCP_COMPANY_FORM
			, "FormReviewStatus" // FCP_DISPOSITION
	};

	public static String[] _aLHRateFields = { "RateSchItemName" // FCP_COMPANY_FORM
			, "RateSchItemID" // FCP_DISPOSITION
			, "RateReviewStatus" }; // FCP_FORM

	public static String[] _aSupportingDocFields = { "FilingSERFFTrackNum",
			"SuppDocName" // FCP_FORM
			, "GenFilingSuppDocComm" // FCP_COMPANY_FORM
			, "SuppDocReviewStatus" }; // FCP_DISPOSITION

	public static String[] _aCompanyFields = { "FilingSERFFTrackNum",
			"CompanyFEINNumber", "CompanyGroupName", "CompanyCoCode" };

	static String[] _aCompanyRateFields = { "RateAffPolicyHolder",
			"RateFilingMethod", "RateOverallRateImpact", "RatePremiumRateChng",
			"RateMaxChange", "RateMinChange", "RateDateCreated",
			"RateUserLastMod", "RateUserCreated", "RateDateLastMod",
			"RatePremium" };

	static String[] _aPCDispositionFields = { "FilingSERFFTrackNum",
			"DispositionDate", "DispositionStatus", "DispEffImplDateNew",
			"DispEffImplDateRenewal" };

	static String[] _aLHDispositionFields = { "FilingSERFFTrackNum",
			"DispositionDate", "DispositionStatus", };

	static String[] _aObjectionLetterFields = { "FilingSERFFTrackNum",
			"ObjectDateSubmitted", "ObjLetterRespondByDate",
			"ObjectIntroduction" };

	static String[] _aFilingFields = {
			"DateLastModified",
			"DateCreated",
			"RefNum",
			"StateStatusDateChng",
			"FeeCalcExplan"
			// , "Retalitory"
			, "DatePrimRevAssigned", "IsFeeRequired", "EffImpReqRenewOnAppr",
			"contactfaxext", "EffImpDateReqRenew", "ProjectNum",
			"CompTrackNum", "FeeAmount", "FilingUserCreatedName",
			"PaperComponentInfo", "PaperFilingLocation",
			"PaperFilingReference", "PaperMailDate", "ProjectNum",
			"DeemerDate", "OthFilingModeExp", "LeadFormNumber",
			"UserSubmitted", "FilingID", "IsRefFiling", "IsElectronic",
			"ContactPhoneExt", "EffImpReqNewOnAppr", "PaperReviewerName",
			"StateFilingDesc", "ProjectName", "ProductName",
			"ContactPostalCode", "ContactDistrictAbrv", "ContactCity",
			"StateStatus", "ContactFirstName", "DomicileStatusComm",
			"SubmissionDate", "PrimaryReviewer", "FilingType", "SubTOIName",
			"TOIName", "LeadFormNumber", "ContactAddress1", "FilingDesc",
			"PaperReviewerFax", "ContactAddress3", "ContactAddress2",
			"ContactEmail", "ContactTitle", "StateTrackNum",
			"EffImpDateReqNew", "FilingSERFFTrackNum", "DomicileFilingStatus",
			"RefTitle", "Contactfax", "ContactPhone", "RefOrg",
			"SERFFStatusDateChg", "ContactCountryName", "StatusCode",
			"PaperReviewerPhone", "PaperReviewerFax", "ProductName",
			"StateFilingDesc", "SubmissionDate", "PrimaryReviewer",
			"EffImpDateReqNew", "SubTOIName", "DomicileFilingStatus",
			"FilingID", "IsElectronic", "FilingUserCreatedName",
			"OthFilingModeExp", "PaperReviewerPhone", "PaperReviewerName",
			"FilingType", "LahGrpMarketExpl", "DomicileApproveDate",
			"CompTrackNum", "StateStatus", "StateTrackNum", "StatusCode",
			"FilingDesc", "UserSubmitted", "LahGrpMarketSize", "LahMarketType",
			"SubmissionType", "RequestedFilingMode", "SERFFStatusDateChg",
			"DomicileStatusComm", "FilingSERFFTrackNum", "UserSubmitted",
			"PaperFilingReference", "PaperMailDate", "PaperReviewerFax",
			"PaperReviewerName", "PaperMailDate", "PaperFilingLocation",
			"TOIName", "PaperReviewerPhone", "FilingID", "DatePrimRevAssigned",
			"DeemerDate", "EffImpReqNewOnAppr", "OthFilingModeExp",
			"LeadFormNumber", "PaperFilingReference", "FilingUserCreatedName",
			"PaperComponentInfo", "PaperFilingLocation", "PaperComponentInfo",
			"ContactLastName", "LahGrpMarketType", "DateCreated",
			"DateLastModified", "DatePrimRevAssigned", "DeemerDate",
			"EffImpReqNewOnAppr", "IsElectronic", "LeadFormNumber" };

	static String[] _aValidObjects = { "FILING", "COMPANY", "FORMSCHEDULE",
			"RATESCHEDULE", "CHECKS", "COMPANYRATE", "DISPOSITION",
			"AmendLetterSchItem", "NoteToFiler", "NoteToReviewer", "Objection",
			"ObjectionItem", "ObjectionSchItem", "RateSchAffFormNum",
			"ResponseLetter", "ResponseObjection", "ReviewerNote",
			"SF_Amendment_Letter", "StateParameters", "SupportDocs" };

	public static void setServers(String filingServer, String pdfServer,
			String correspondenceServer, String attachmentServer) {
		_filing_server = filingServer;
		_pdf_server = pdfServer;
		_correspondence_server = correspondenceServer;
		_attachment_server = attachmentServer;
	}

	public static Vector<QueryResult> runQueryWithRetry(String userName,
			String password, String condition, String[] aFieldsToFind,
			String form) {
		Vector<QueryResult> vtRet = null;
		try {
			for (int i = 0; i < 3; i++) {
				try {
					// Filing object
					Filing myFiling = getFiling(userName, password);

					QueryFiling myQueryFiling = new QueryFiling();
					myQueryFiling.setForm(form);

					myQueryFiling.setCondition(condition);
					myQueryFiling.setAttachmentNamePattern("*");
					Fields fieldsToFind = new Fields();

					fieldsToFind.setField(aFieldsToFind);
					myQueryFiling.setFields(fieldsToFind);
					// System.out.println("runQuery Calling the web
					// service...");

					QueryFilingResponse queryFilingResponse = myFiling
							.queryFiling(myQueryFiling);

					if (queryFilingResponse != null) {
						vtRet = getQueryResult(queryFilingResponse);
//						printResult(vtRet, aFieldsToFind);
					} else {
						System.out
								.println("runQuery Failed to obtain a response from the web service.");
					}
					break;

				} catch (Exception e) {
					long waitTime = HandleException(e);
					vtRet = null;
					if (waitTime > 1) {
						// System.out.println(BasicTask.getSysTime() + " ");
						System.out.println("Wait " + waitTime
								+ " ms for next time slot...");
						Thread.sleep(waitTime + 500);
					} else {
						e.printStackTrace();
						System.out.println("Retry " + i);
						System.out.println(condition);
						continue;
					}
				}
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		return vtRet;
	}

	public static Vector<QueryResult> runQuery(String userName,
			String password, String condition, String[] aFieldsToFind,
			String form) {
		Vector<QueryResult> vtRet = null;
		try {
			// Filing object
			Filing myFiling = getFiling(userName, password);

			QueryFiling myQueryFiling = new QueryFiling();
			myQueryFiling.setForm(form);

			myQueryFiling.setCondition(condition);
			myQueryFiling.setAttachmentNamePattern("*");

			Fields fieldsToFind = new Fields();

			fieldsToFind.setField(aFieldsToFind);
			myQueryFiling.setFields(fieldsToFind);
			System.out.println("runQuery Calling the web service...");

			QueryFilingResponse queryFilingResponse = myFiling
					.queryFiling(myQueryFiling);

			if (queryFilingResponse != null) {
				vtRet = getQueryResult(queryFilingResponse);
				// printResult(vtRet, aFieldsToFind);
			} else {
				System.out
						.println("runQuery Failed to obtain a response from the web service.");
			}

		} catch (Exception e) {
			HandleException(e);
		}
		return vtRet;
	}

	public static void printResult(Vector vtRet, String[] aFieldsToFind) {
		if (vtRet != null) {
			for (int i = 0; i < vtRet.size(); i++) {
				QueryResult rs = (QueryResult) (vtRet.elementAt(i));
				org.naic.serff.stateapi.service.Value[] values = rs.getValues();
				System.out.println("========== Row " + i + "==========");
				for (int j = 0; j < aFieldsToFind.length; j++) {
					System.out.print(aFieldsToFind[j] + ": ");
					String[] vs = values[j].getVs();
					for (int k = 0; k < vs.length; k++) {
						System.out.print(vs[k] + ", ");
					}
					System.out.println("");
				}
				System.out.println("========= Row" + i + " End ==========");
			}
		}
	}

	public static Vector<QueryResult> getQueryResult(
			QueryFilingResponse queryFilingResponse) {
		Vector<QueryResult> vtRet = null;
		try {

			ResultTableType rtt = queryFilingResponse.getResultTable();
			org.naic.serff.stateapi.service.Row[] row = rtt.getRow();

			vtRet = new Vector<QueryResult>();
			if (row.length != 0) {
				for (int i = 0; i < row.length; i++) {
					org.naic.serff.stateapi.service.Value[] values = row[i]
							.getValue();
					vtRet.addElement(new QueryResult(row[i].getValue(), row[i]
							.getAttachmentIdentifier()));
				} // end for
			} // end if

		} catch (Exception e) {
			HandleException(e);
		}
		return vtRet;

	}

	public static Vector<QueryResult> getNewFilingNumber(String userName,
			String password, String startDate) {
		String condition = "SubmissionDate >= date '" + startDate + "'";
		String[] aFieldsToFind = new String[2];
		aFieldsToFind[0] = "FilingSERFFTrackNum";
		aFieldsToFind[1] = "DateCreated";
		return runQuery(userName, password, condition, aFieldsToFind, "FILING");
	}

	public static Vector<QueryResult> getAllFilingBetween(String userName,
			String password, String startDate, String endDate) {
		return getAllFilingBetween( userName,
				 password,  startDate,  endDate,false);
	}
	
	public static Vector<QueryResult> getAllFilingBetween(String userName,
			String password, String startDate, String endDate,boolean bUseModifiedDate) {
		String condition = bUseModifiedDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate);
		String[] aFieldsToFind = new String[9];
		aFieldsToFind[0] = "FilingSERFFTrackNum";
		aFieldsToFind[1] = "SubmissionDate";
		aFieldsToFind[2] = "PrimaryReviewer";
		aFieldsToFind[3] = "StatusCode";
		aFieldsToFind[4] = "StateTrackNum";
		aFieldsToFind[5] = "StateStatus";
		aFieldsToFind[6] = "TOIName";
		aFieldsToFind[7] = "SubTOIName";
		aFieldsToFind[8] = "FilingType";
		return runQueryWithRetry(userName, password, condition, aFieldsToFind,
				"FILING");
	}
	
	public static ArrayList<String> getReadyForKiraPCFilings(String userName,String password){
//		String condition = "StateStatus='"+READYFORKIRA+"' AND StatusCode<>'Closed'";	// can not create objection letter for closed cases
		String condition = "StateStatus='"+READYFORKIRA+"' AND StatusCode<>'Closed'";
		String[] aFieldsToFind = new String[2];
		aFieldsToFind[0] = "FilingSERFFTrackNum";
		aFieldsToFind[1] = "TOIName";
		Vector<QueryResult> result =  runQuery(userName, password, condition, aFieldsToFind,
				"FILING");

		ArrayList<String> ret = new ArrayList();
		for (int i=0;i<result.size();i++) {
			org.naic.serff.stateapi.service.Value[] values = result
					.elementAt(i).getValues();
			String toi = values[1].getVs()[0];
			int p = toi.indexOf(' ');
			toi = toi.substring(0, p);
			ret.add(values[0].getVs()[0]+"-"+toi);
		}
		return ret;
	}

	public static Vector<QueryResult> getLHFlingFormInfoBetween(
			String userName, String password, String startDate, String endDate) {
		return getLHFlingFormInfoBetween(userName, password, startDate, endDate,false);
	}

	public static Vector<QueryResult> getLHFlingFormInfoBetween(
			String userName, String password, String startDate, String endDate,boolean bUseModifiedDate) {
		return getFlingFormInfoBetween(userName, password, startDate, endDate,
				_aLHFormFields,bUseModifiedDate);
	}
	public static Vector<QueryResult> getPCFlingFormInfoBetween(
			String userName, String password, String startDate, String endDate,boolean bUseModifiedDate) {
		return getFlingFormInfoBetween(userName, password, startDate, endDate,
				_aPCFormFields,bUseModifiedDate);
	}

	public static Vector<QueryResult> getFlingFormInfoBetween(String userName,
			String password, String startDate, String endDate,
			String[] aFormFields,boolean bUseModifiedDate) {
		return runQueryWithRetry(userName, password,
				bUseModifiedDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate), aFormFields,
				"FORMSCHEDULE");
	}

	// Hard coded exception case, there're special character in the user comment field caused xml parser exception.
	public static Vector<QueryResult> getSupportingDocBetween(String userName,
			String password, String startDate, String endDate,boolean bUseModifiedDate) {
		return runQueryWithRetry(userName, password,
				bUseModifiedDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate)+" and FilingSERFFTrackNum<>'UCCA-128983509'",
				_aSupportingDocFields, "SUPPORTDOCS");
	}

	public static Vector<QueryResult> getCompanyInfoBetween(String userName,
			String password, String startDate, String endDate,boolean bUseModifiedDate) {
		return runQueryWithRetry(userName, password,
				bUseModifiedDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate),
				_aCompanyFields, "COMPANY");
	}

	
	public static Vector<QueryResult> getCompanyInfo(String userName,
			String password, String serffNo) {
		String condition = "FilingSERFFTrackNum = '" + serffNo + "'";
		return runQueryWithRetry(userName, password, condition, _aCompanyFields, "COMPANY");
	}
	
	
	
	
	public static String getSubmissionDateCondition(String startDate,
			String endDate) {
		return "SubmissionDate <= date '" + endDate
				+ "' and SubmissionDate > date '" + startDate + "'";
	}

	public static String getModifiedDateCondition(String startDate,
			String endDate) {
		return "DateLastModified <= date '" + endDate
				+ "' and DateLastModified > date '" + startDate + "'";
	}

	public static Vector<QueryResult> getClosedFilingBetween(String userName,
			String password, String startDate, String endDate) {
		String condition = getSubmissionDateCondition(startDate, endDate)
				+ "' AND StatusCode = 'Closed'";
		String[] aFieldsToFind = new String[3];
		aFieldsToFind[0] = "FilingSERFFTrackNum";
		aFieldsToFind[1] = "SubmissionDate";
		aFieldsToFind[2] = "PrimaryReviewer";
		return runQueryWithRetry(userName, password, condition, aFieldsToFind,
				"FILING");
	}

	public static Vector<QueryResult> getPCFilingBetween(String userName,
			String password, String startDate, String endDate,boolean bUseDispDate) {
		// SubmissionDate for PC
		String[] aFieldsToFind = new String[6];
		aFieldsToFind[0] = "FilingSERFFTrackNum";
		aFieldsToFind[1] = "StateTrackNum";
		aFieldsToFind[2] = "PrimaryReviewer";
		aFieldsToFind[3] = "SubmissionDate";
		aFieldsToFind[4] = "TOIName";
		aFieldsToFind[5] = "SubTOIName";
		return runQueryWithRetry(userName, password,
				bUseDispDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate), aFieldsToFind,
				"FILING");
	}

	// Get all filings after specific date
	public static Vector getNewFilingNumber(String userName, String password,
			Date startDate) {

		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(
				"yyyy-MM-dd");
		String strDate = format.format(startDate);
		return getNewFilingNumber(userName, password, strDate);
	}

	public static java.sql.Timestamp getTimeFromString(String dateStr) {
		java.sql.Timestamp ret = null;
		try {
			if (dateStr != null) {
				dateStr = dateStr.trim();
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(
						"EEE MMM dd HH:mm:ss zzz yyyy");
				java.util.Date date = sdf.parse(dateStr);
				// Tuncate time to 00:00:00
				/*
				 * GregorianCalendar cal = new GregorianCalendar();
				 * cal.setTimeInMillis(date.getTime());
				 * cal.set(GregorianCalendar.HOUR,0);
				 * cal.set(GregorianCalendar.MINUTE,0);
				 * cal.set(GregorianCalendar.SECOND,0);
				 */
				// ret = new java.sql.Timestamp(cal.getTimeInMillis());
				ret = new java.sql.Timestamp(date.getTime());
			}
		} catch (Exception e) {
			e.printStackTrace();
			ret = null;
		}
		return ret;
	}

	// Get closed filings created after specific date
	public static Vector<QueryResult> getClosedFilingNumber(String userName,
			String password, String startDate) {
		String condition = "SubmissionDate >= date '" + startDate
				+ "' AND StatusCode = 'Closed'";
		String[] aFieldsToFind = new String[3];
		aFieldsToFind[0] = "FilingSERFFTrackNum";
		aFieldsToFind[1] = "SubmissionDate";
		aFieldsToFind[2] = "PrimaryReviewer";
		return runQuery(userName, password, condition, aFieldsToFind, "FILING");
	}

	public static Vector getClosedFilingNumber(String userName,
			String password, Date startDate) {

		java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(
				"yyyy-MM-dd");
		String strDate = format.format(startDate);
		return getClosedFilingNumber(userName, password, strDate);
	}

	public static Vector getLHFlingGeneralInfo(String userName,
			String password, String serffNo) {
		return getFlingInfo(userName, password, serffNo, _aLHFilingFields,
				"FILING");
	}

	public static Vector getLHFlingFormInfo(String userName, String password,
			String serffNo) {
		return getFlingInfo(userName, password, serffNo, _aLHFormFields,
				"FORMSCHEDULE");
	}

	public static Vector<QueryResult> getSupportingDocInfo(String userName,
			String password, String serffNo) {
		String condition = "FilingSERFFTrackNum = '" + serffNo + "'";
		return runQueryWithRetry(userName, password, condition,
				_aSupportingDocFields, "SUPPORTDOCS");
	}

	public static Vector getLHFlingRateInfo(String userName, String password,
			String serffNo) {
		return getFlingInfo(userName, password, serffNo, _aLHRateFields,
				"RATESCHEDULE");
	}

	public static Vector getFlingCompanyInfo(String userName, String password,
			String serffNo) {
		return getFlingInfo(userName, password, serffNo, _aCompanyFields,
				"COMPANY");
	}

	public static Vector getLHDispositionInfo(String userName, String password,
			String serffNo) {
		return getFlingInfo(userName, password, serffNo, _aLHDispositionFields,
				"DISPOSITION");
	}

	public static Vector<QueryResult> getPCDispositionInfo(String userName,
			String password, String endDate, String startDate,boolean bUseDispDate) {

		return runQueryWithRetry(userName, password,
				bUseDispDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate),
				_aPCDispositionFields, "DISPOSITION");
	}

	public static Vector<QueryResult> getObjectionLetterInfo(String userName,
			String password, String endDate, String startDate) {

		return runQueryWithRetry(userName, password,
				getSubmissionDateCondition(startDate, endDate),
				_aObjectionLetterFields, "OBJECTION");
	}

	public static Vector<QueryResult> getLHDispositionInfo(String userName,
			String password, String startDate, String endDate) {
		return getLHDispositionInfo( userName, password,  startDate,  endDate,false);
	}

	public static Vector<QueryResult> getLHDispositionInfo(String userName,
			String password, String startDate, String endDate,boolean bUseModifiedDate) {

		return runQueryWithRetry(userName, password,
				bUseModifiedDate?getModifiedDateCondition(startDate, endDate):getSubmissionDateCondition(startDate, endDate),
				_aLHDispositionFields, "DISPOSITION");
	}

	public static Vector<QueryResult> getFlingInfo(String userName,
			String password, String serffNo, String[] aFieldsToFind, String type) {
		String condition = "FilingSERFFTrackNum = '" + serffNo + "'";
		return runQueryWithRetry(userName, password, condition, aFieldsToFind,
				type);
	}

	static Filing _myFiling = null;

	public static Filing getFiling(String userName, String password)
			throws Exception {
		if (_myFiling == null) {
			StateApiService_Impl implementation = new StateApiService_Impl();
			_myFiling = implementation.getFiling();
			// The below code sets your userName and Password from the
			// user.properties file
			((Stub) _myFiling)._setProperty(Stub.USERNAME_PROPERTY, userName);
			((Stub) _myFiling)._setProperty(Stub.PASSWORD_PROPERTY, password);
			((Stub) _myFiling)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
					_filing_server);
		}
		return _myFiling;
	}

	static Correspondence _myCorrespondence=null;
	public static Correspondence getCorrespondence(String userName, String password)
			throws Exception {
		if (_myCorrespondence == null) {
			StateApiService_Impl implementation = new StateApiService_Impl();
			_myCorrespondence = implementation.getCorrespondence();
			// The below code sets your userName and Password from the
			// user.properties file
			((Stub) _myCorrespondence)._setProperty(Stub.USERNAME_PROPERTY, userName);
			((Stub) _myCorrespondence)._setProperty(Stub.PASSWORD_PROPERTY, password);
			((Stub) _myCorrespondence)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
					_correspondence_server);
		}
		return _myCorrespondence;
	}

	
	public static Filing getFiling1(String userName, String password)
			throws Exception {
		StateApiService_Impl implementation = new StateApiService_Impl();
		Filing myFiling = implementation.getFiling();
		// The below code sets your userName and Password from the
		// user.properties file
		((Stub) myFiling)._setProperty(Stub.USERNAME_PROPERTY, userName);
		((Stub) myFiling)._setProperty(Stub.PASSWORD_PROPERTY, password);
		((Stub) myFiling)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
				_filing_server);
		return myFiling;
	}

	public static String setReviewerName(String userName, String password,
			String filingNo, String name) {
		String ret = null;
		try {
			// Filing object

			Filing myFiling = getFiling(userName, password);
			ret = setReviewerName(myFiling, filingNo, name);
			// Try again with tail
			if (ret == null && (filingNo.indexOf("/00-00/00-00/00") < 0)) {
				filingNo += "/00-00/00-00/00";
				ret = setReviewerName(myFiling, filingNo, name);
			}
		} catch (Exception e) {
			HandleException(e);
		}
		return ret;
	}

	public static String setReviewerNameWithRetry(String userName,
			String password, String filingNo, String name) {
		String ret = null;
		try {
			// Filing object

			Filing myFiling = getFiling(userName, password);
			ret = setReviewerName(myFiling, filingNo, name);
			// Try again with tail
			if (ret == null && (filingNo.indexOf("/00-00/00-00/00") < 0)) {
				filingNo += "/00-00/00-00/00";
				ret = setReviewerName(myFiling, filingNo, name);
			}
		} catch (Exception e) {
			ret = null;
		}
		return ret;
	}

	public static String setReviewerName(Filing myFiling, String filingNo,
			String name) {
		String ret = null;
		for (int i = 0; i < 10; i++) {
			try {
				// Filing object

				SetReviewerName setName = new SetReviewerName();
				setName.setPrimaryReviewerName(name);
				setName.setSerffTrackingNumber(filingNo);

				ret = myFiling.setReviewerName(setName);
				break;
			} catch (Exception e) {
				HandleException(e);
				ret = null;
				try {
					// Sleep .5 second
					Thread.sleep(500);
				} catch (Exception ex) {
				}
				System.out.println("Retry assign: " + i);
				continue;
			}
		}
		return ret;
	}

	public static String setStateTrackingNumberWithRetry(String userName,
			String password, String filingNo, String stateNo) {
		String ret = null;
		try {
			for (int i = 0; i < 3; i++) {
				try {
					// Filing object
					Filing myFiling = getFiling(userName, password);
					ret = setStateTrackingNumber(myFiling, filingNo, stateNo);
					break;
				} catch (Exception e1) {
					ret = null;
					long waitTime = HandleException(e1);
					if (waitTime > 1) {
						// System.out.println(BasicTask.getSysTime() + " ");
						System.out.println("Wait " + waitTime
								+ " ms for next time slot...");
						Thread.sleep(waitTime + 500);
					} else {
						System.out.println("Retry Assignment " + i);
						continue;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public static String setStateTrackingNumber(String userName,
			String password, String filingNo, String stateNo) {
		String ret = null;
		try {
			// Filing object
			Filing myFiling = getFiling(userName, password);
			ret = setStateTrackingNumber(myFiling, filingNo, stateNo);
		} catch (Exception e) {
			HandleException(e);
		}
		return ret;
	}

	public static String setStateTrackingNumber(Filing myFiling,
			String filingNo, String stateNo) throws Exception {
		String ret = null;
		// Filing object

		SetStateTrackingNumber number = new SetStateTrackingNumber();
		number.setStateTrackingNumber(stateNo);
		number.setSerffTrackingNumber(filingNo);

		ret = myFiling.setStateTrackingNumber(number);
		return ret;
	}

	public static String setStateStatus(String userName, String password,
			String filingNo, String stateStatus) {
		String ret = null;
		try {
			// Filing object
			Filing myFiling = getFiling(userName, password);
			
			SetStateStatus status = new SetStateStatus(filingNo,stateStatus);
			ret = myFiling.setStateStatus(status);
			
		} catch (Exception e) {
			HandleException(e);
		}
		return ret;
	}

	public static String setStateStatus(Filing myFiling, String filingNo,
			String stateStatus) {
		String ret = null;
		try {
			// Filing object

			SetStateStatus status = new SetStateStatus();
			status.setStateStatus(stateStatus);
			status.setSerffTrackingNumber(filingNo);

			ret = myFiling.setStateStatus(status);
		} catch (Exception e) {
			HandleException(e);
		}
		return ret;
	}

	public static long HandleException(Exception e) {
		long waitTime = -1;
		if (e instanceof ClientException) {
			System.err
					.println("\n----------------------------------------------");
			System.err.println("BEGIN Web Service Message");
			System.err.println(e.getMessage());
			System.err.println(((ClientException) e).getCode());
			System.err.println("ClientException, Code "
					+ ((ClientException) e).getCode());
			e.printStackTrace();
			System.err.println("END Web Service Message");
			System.err
					.println("-------------------------------------------------");
		} else if (e instanceof ServerException) {
			System.err
					.println("\n-----------------------------------------------");
			System.err.println("BEGIN Web Service Message");
			System.err.println(e.getMessage());
			System.err.println(((ServerException) e).getCode());
			System.err.println("ServerException, Code "
					+ ((ServerException) e).getCode());
			System.err.println("END Web Service Message (S)");
			System.err
					.println("-------------------------------------------------");
		} else if (e instanceof CallRateExceededException) {
			System.err
					.println("\n-----------------------------------------------");
			System.err.println("BEGIN Web Service Message: ");
			System.err.println(e.getMessage());
			waitTime = ((CallRateExceededException) e)
					.getMillisecondsUntilNextAllowedCall();
			String message = "Another call can be made in " + waitTime
					+ " milliseconds";

			System.err.println(message);
			System.err.println("CallRateExceededException, " + message);
			Date dt = GregorianCalendar.getInstance().getTime();
			long nextcall = dt.getTime() + waitTime;
			Date dt2 = new Date(nextcall);
			System.err.println("Next call is at: " + dt2);
			System.err.println("END Web Service Message");
			System.err
					.println("-------------------------------------------------");
		} else {
			System.err
					.println("\n-----------------------------------------------");
			System.err.println("BEGIN: UNEXPECTED ERROR!!!");
			System.err.println("ERROR:\n");
			System.err.println(e.getMessage());
			showErrorMessage(e.getMessage());
			e.printStackTrace();
			System.err.println("END: UNEXPECTED ERROR!!!");
			System.err
					.println("-------------------------------------------------");
		}
		return waitTime;
	}

	public static void showErrorMessage(String message) {
		/*
		 * JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.
		 * INFORMATION_MESSAGE);
		 */
	}

	public static boolean downloadFile(String userName, String password,
			String name, String key, String outputDir) {
		boolean ret = false;
		try {
			AttachmentClient attachmentClient = new AttachmentClient(userName,
					password, _attachment_server, 1000000, System.out);
			attachmentClient.setOutputDirectory(outputDir);
			attachmentClient.download(name, key);
			ret = true;
		} catch (Exception e) {
			e.printStackTrace();

		}
		return ret;
	}

	static public void main(String[] args) {
		System.out.println(downloadFilingAsPDF("ctsapiLH","pass1ctsapiLH","BNLB-126433843","c:\\temp"));
		System.exit(0);
	}
	
	
	public static String downloadFilingAsPDF(String userName, String password,
			String serffNo,String outputDir)
	{
		Pdf pdfTool = null;
		AttachmentIdentifier aID=null;
		String id=null;
		String ret= null;
		try
		{
			StateApiService_Impl implementation = new StateApiService_Impl();
			pdfTool = implementation.getPdf();
			((Stub) pdfTool)._setProperty(Stub.USERNAME_PROPERTY, userName);
			((Stub) pdfTool)._setProperty(Stub.PASSWORD_PROPERTY, password);
			((Stub) pdfTool)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
					_pdf_server);
			aID = pdfTool.requestPdf(serffNo);
			if (aID != null)
			{
				id = aID.getAttachmentId();
				ret = aID.getAttachmentName();
				downloadFile(userName, password, aID.getAttachmentName(),id,outputDir);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			ret = null;
		}
		finally
		{
			if (pdfTool!=null && aID != null)
			{
				try
				{
					pdfTool.finalizePdf(id);
				}
				catch(Exception ex)
				{
					ex.printStackTrace();
				}
			}
		}
		return ret;
	}
	
	public static String submitObjectionLetter(String userName,String password,String serffNo,String objectionLetterStatus, Calendar effectiveDate, String introductionText, String conclusionText, Calendar respondByDate,String comment) throws Exception
	{
		String[] aFileNames = new String[] {};
		org.naic.serff.stateapi.service.AttachmentInputGroup attachments = new AttachmentInputGroup(aFileNames);
		org.naic.serff.stateapi.service.Objection sampleobj = new org.naic.serff.stateapi.service.Objection(comment,new String[] {});
		org.naic.serff.stateapi.service.Objection[] objs = new org.naic.serff.stateapi.service.Objection[] {sampleobj};
    	org.naic.serff.stateapi.service.ObjectionGroup objections = new org.naic.serff.stateapi.service.ObjectionGroup(objs); 

		ObjectionLetter letter = new ObjectionLetter(objectionLetterStatus, effectiveDate, introductionText, conclusionText, respondByDate, null,objections);
		ObjectionLetterRequest request = new ObjectionLetterRequest(serffNo,letter);
		// Filing object
		Correspondence correspondence = getCorrespondence(userName, password);
		
    	ObjectionLetterResponse response = null;
    	try
    	{
    		response = correspondence.submitObjectionLetter( request);
    		if (response!=null)
    			return response.getObjectionLetterId();
    	}
    	catch (org.naic.serff.stateapi.service.ClientException ce) {
    		System.out.println("Client Exception");
    		ce.printStackTrace();
    	}
    	catch (org.naic.serff.stateapi.service.ServerException se) {
    		System.out.println("Server Exception");
    		se.printStackTrace();
    	}
    	catch (org.naic.serff.stateapi.service.CallRateExceededException xe) {
    		System.out.println("Exceeded Quote");
    		xe.printStackTrace();
    	}
    	catch (java.rmi.RemoteException re) {
    		System.out.println("Remote Exception");
    		re.printStackTrace();
    	}
    	catch (Exception ee)
    	{
    		System.out.println("Other Exception");
    		ee.printStackTrace();
    	}
		
		
		
		return null;
	}
}
