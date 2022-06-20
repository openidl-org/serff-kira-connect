package serffproc;

import java.io.PrintStream;

import javax.xml.rpc.Stub;
import org.naic.serff.stateapi.service.CallRateExceededException;
import org.naic.serff.stateapi.service.ClientException;
import org.naic.serff.stateapi.service.ReadBlockResponse;
import org.naic.serff.stateapi.service.ServerException;
import org.naic.serff.stateapi.service.ObjectionLetterResponse;
import org.naic.serff.stateapi.service.ObjectionLetterRequest;
import org.naic.serff.stateapi.service.ObjectionLetter;
import org.naic.serff.stateapi.service.AttachmentInputGroup;

public class CorrespondenceClient {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	
    private String callEndpoint;
    private int clientBlockSize;
    org.naic.serff.stateapi.service.Correspondence correspondance;
    private String outputDirectory = "";
    private String inputDirectory = "";
    private PrintStream out;

    public CorrespondenceClient(String username, String password, String endpoint,PrintStream output)
    {
        callEndpoint = endpoint;
        org.naic.serff.stateapi.service.StateApiService_Impl service =
            new org.naic.serff.stateapi.service.StateApiService_Impl();
        correspondance =  service.getCorrespondence();

        // set the endpoint properties
        ((Stub)correspondance)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
            endpoint);
        ((Stub)correspondance)._setProperty(Stub.USERNAME_PROPERTY, username);
        ((Stub)correspondance)._setProperty(Stub.PASSWORD_PROPERTY, password);
        out = output;
    }
	
    
    // ?? The taget filing cannot have a closed status?
    public  ObjectionLetterResponse submitObjectionLetter(String filingId,java.lang.String objectionLetterStatus, java.util.Calendar effectiveDate, java.lang.String introductionText, java.lang.String conclusionText, java.util.Calendar respondByDate) {
//        public  ObjectionLetterResponse submitObjectionLetter(String filingId,java.lang.String objectionLetterStatus, java.util.Calendar effectiveDate, java.lang.String introductionText, java.lang.String conclusionText, java.util.Calendar respondByDate, org.naic.serff.stateapi.service.AttachmentInputGroup attachments) {
    	// construct objectionletter
    	AttachmentInputGroup attachments = new AttachmentInputGroup();
    	org.naic.serff.stateapi.service.ObjectionGroup objections = new org.naic.serff.stateapi.service.ObjectionGroup(); 
    	ObjectionLetter objectionLetter = new ObjectionLetter(objectionLetterStatus, effectiveDate, introductionText, conclusionText, respondByDate, attachments,objections)  ;
    	// construct request
    	ObjectionLetterRequest request = new ObjectionLetterRequest(filingId, objectionLetter );
    	ObjectionLetterResponse response = null;
    	try
    	{
    		response = correspondance.submitObjectionLetter( request);
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
    	        
    	return response;
    };
    

}
