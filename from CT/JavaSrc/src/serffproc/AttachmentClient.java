package serffproc;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.rmi.RemoteException;

import javax.xml.rpc.Stub;

import org.naic.serff.stateapi.service.AttachmentIdentifier;
import org.naic.serff.stateapi.service.BeginDownloadResponse;
import org.naic.serff.stateapi.service.CallRateExceededException;
import org.naic.serff.stateapi.service.ClientException;
import org.naic.serff.stateapi.service.ReadBlockResponse;
import org.naic.serff.stateapi.service.ServerException;


public class AttachmentClient
{

    private PrintStream out;
    private String callEndpoint;
    private int clientBlockSize;
    org.naic.serff.stateapi.service.AttachmentPort attachmentPort;
    private String outputDirectory = "";
    private String inputDirectory = "";

    public AttachmentClient(String username, String password, String endpoint,
        int client_block_size, PrintStream output)
    {
        callEndpoint = endpoint;
        clientBlockSize = client_block_size;
        org.naic.serff.stateapi.service.StateApiService_Impl service =
            new org.naic.serff.stateapi.service.StateApiService_Impl();
        attachmentPort =  service.getAttachment();

        // set the endpoint properties
        ((Stub)attachmentPort)._setProperty(Stub.ENDPOINT_ADDRESS_PROPERTY,
            endpoint);
        ((Stub)attachmentPort)._setProperty(Stub.USERNAME_PROPERTY, username);
        ((Stub)attachmentPort)._setProperty(Stub.PASSWORD_PROPERTY, password);
        out = output;
    }

    public void download(AttachmentIdentifier[] s_ais) throws RemoteException, ClientException, ServerException
    {
        for (int i = 0; i < s_ais.length; ++i)
        {
            download(s_ais[i].getAttachmentName(), s_ais[i].getAttachmentId());
        }
    }

    public void download(String name, String external_id) throws RemoteException, ClientException, ServerException
    {
        out.println("Calling the download service at " + callEndpoint);
        FileOutputStream fos = null;
        try
        {
            out.println("Begin download. name: " + name + ", id: " + external_id);
            fos = new FileOutputStream(outputDirectory + "/" + name);
            BeginDownloadResponse bdr =
                attachmentPort.beginDownload(external_id, BigInteger.valueOf(clientBlockSize));


            BigInteger transfer_block_size = bdr.getBlockSize();
            int file_size = bdr.getFilesize().intValue();
            out.println("  File size: " + file_size +
                ", transfer block size: " + transfer_block_size);
            int offset = 0;
            int cnt = 0;
            ReadBlockResponse rbr =
                attachmentPort.readBlock(external_id,
                    BigInteger.valueOf(offset), transfer_block_size);
            while (offset < file_size && rbr.getData() != null && rbr.getData().length > 0)
            {
                ++cnt;
                out.println("  Read block: " + cnt + ", size: " + rbr.getData().length
                    + ", md5sum: " + rbr.getMd5());
                offset += rbr.getData().length;
                fos.write(rbr.getData());
                rbr =
                    attachmentPort.readBlock(external_id,
                        BigInteger.valueOf(offset), transfer_block_size);
            }
            String edr_md5sum = attachmentPort.endDownload(external_id);
            out.println("End download. name: " + name + ", md5sum: " +
                edr_md5sum + ", id: " + external_id);
        }
        catch(IOException ioe)
        {
            throw new RuntimeException("Failed creating/reading the download file.", ioe);
        }
        finally
        {
            try
            {
                if (fos != null)
                {
                    fos.close();
                }
            }
            catch(IOException ioe)
            {
                throw new RuntimeException("Failed closing download file.", ioe);
            }
        }
    }


    public void upload(AttachmentIdentifier[] s_ais) throws RemoteException, ClientException, ServerException,
        CallRateExceededException, RemoteException
    {
        for (int i = 0; i < s_ais.length; ++i)
        {
            upload(s_ais[i].getAttachmentName(), s_ais[i].getAttachmentId());
        }
    }

    public void upload(String name, String external_id) throws RemoteException, ClientException, ServerException,
        CallRateExceededException, RemoteException
    {
        out.println("Calling the upload service at " + callEndpoint);
        File ifile = null;
        FileInputStream fis = null;
        out.println("Begin upload. name: " + name + ", id: " + external_id);
        ifile = new File(inputDirectory + "/" + name);
        if (!ifile.isFile())
        {
            throw new RuntimeException("The file, " + inputDirectory + "/" + name +
                " does not exist.");
        }
        try
        {
            fis = new FileInputStream(ifile);
            BigInteger max_server_transfer_size =
                attachmentPort.beginUpload(external_id, BigInteger.valueOf(ifile.length()));
            int transfer_block_size = Math.min(clientBlockSize, max_server_transfer_size.intValue());
            out.println("  File size: " + ifile.length() +
                ", transfer block size: " + transfer_block_size);
            int offset = 0;
            int cnt = 0;
            byte[] data = new byte[transfer_block_size];
            int data_size = fis.read(data);
            while (data_size != -1)
            {
                String md5sum = writeData(transfer_block_size, data, data_size, external_id, offset);
                ++cnt;
                out.println("  Write block: " + cnt + ", size: " + data_size
                    + ", md5sum: " + md5sum);
                offset += data_size;
                data_size = fis.read(data);
            }
            String er_md5sum = attachmentPort.endUpload(external_id);
            out.println("End upload. name: " + name + ", md5sum: " +
                er_md5sum + ", id: " + external_id);
        }
        catch(IOException ioe)
        {
            throw new RuntimeException("Failed creating/reading the upload file.", ioe);
        }
        finally
        {
            try
            {
                if (fis != null)
                {
                    fis.close();
                }
            }
            catch(IOException ioe)
            {
                throw new RuntimeException("Failed closing download file.", ioe);
            }
        }
    }

    private String writeData(int transfer_block_size, byte[] data, int data_size,
        String external_id, int offset) throws RemoteException, ClientException, ServerException,
        RemoteException
    {
        byte[] write_data = data;
        if (data_size != -1 && data_size != data.length)
        {
            write_data = new byte[data_size];
            System.arraycopy(data, 0, write_data, 0, data_size);
        }
        else
        {
            write_data = data;
        }
        return attachmentPort.writeBlock(external_id, BigInteger.valueOf(offset), write_data);
    }

    public String getOutputDirectory()
    {
        return outputDirectory;
    }

    public void setOutputDirectory(String dir)
    {
        outputDirectory = dir;
    }

    public String getInputDirectory()
    {
        return inputDirectory;
    }

    public void setInputDirectory(String dir)
    {
        inputDirectory = dir;
    }

    public void setOut(PrintStream output)
    {
        out = output;
    }


}  //END public class AttachmentClient

