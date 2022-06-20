package serffproc;

import java.util.*;
import javax.xml.rpc.*;

import org.naic.serff.stateapi.service.*;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2003</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class QueryResult {
    org.naic.serff.stateapi.service.Value[] aValues = null;
    AttachmentIdentifier[] aAttachments = null;

    public org.naic.serff.stateapi.service.Value[] getValues()
    {
        return aValues;
    }

    public AttachmentIdentifier[] getAttachments()
    {
        return aAttachments;
    }

    public QueryResult(org.naic.serff.stateapi.service.Value[] aValue,AttachmentIdentifier[] aAttach)
    {
        aValues = aValue;
        aAttachments = aAttach;
    }

}
