package directoryencryption;

import java.io.*;
import java.util.Date;

/**
 *
 * @author scottl
 */
public class DirectoryEncryption {
    private static final boolean debug = false;
    private static final boolean showStatistics = true;
    private long bytesProcessed = 0;
    private long filesProcessed = 0;
    private static Date startTime;
    
    
    public DirectoryEncryption()
    {
        startTime = new Date();
    }
    
    
    public boolean encryptDirectory(File directory)
    {
        boolean returnVal = true;
        
        if (!directory.isDirectory())
            return false;
        for (File file : directory.listFiles())
            if (!file.isDirectory())
            {
                if (!encryptFile(file))
                    returnVal = false;
            }
            else
                encryptDirectory(file);

        return returnVal;
    }
    
    public boolean encryptFile(File file)
    {
        try
            {invertFile(file);}
        catch(IOException e)
        {
            if (debug)
                e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    public boolean decryptFile(File file)
    {
        try
            {invertFile(file);}
        catch(IOException e)
        {
            if (debug)
                e.printStackTrace();
            return false;
        }
        
        return true;
    }
    
    /**
     * Flips ever bit in a file.
     * @param path Path to file to invert.
     * @throws IOException Throws IOException if there is an error during read or write.
     * TODO: add file locking support (obvious joke: "just for the flock of it").
     */
    public void invertFile(File file) throws IOException
    {
        checkFile(file);
        if (file.isDirectory())
            return;
        
        BufferedInputStream inputStream = null;
        BufferedOutputStream outputStream = null;
        try
        {
            inputStream = new BufferedInputStream(new FileInputStream(file));
            File tempFile = File.createTempFile("direnc", String.valueOf(filesProcessed));
            outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            tempFile.deleteOnExit();
            
            byte[] bytes = new byte[4096];
            int numBytesRead = 0;
            // read each byte, flip it and write it to temp file (so we don't run out of memory on large files).
            while((numBytesRead = inputStream.read(bytes)) > -1)
            {
                for(int i = 0; i < numBytesRead; i++)
                    bytes[i]= (byte)~bytes[i];
                outputStream.write(bytes, 0, numBytesRead);
            }
            
            inputStream.close();
            outputStream.close();
            
            // write changes back to original file.  This saves perms that a rename won't.
            inputStream = new BufferedInputStream(new FileInputStream(tempFile));
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
            while((numBytesRead = inputStream.read(bytes)) > -1)
                outputStream.write(bytes, 0, numBytesRead);
        }
        catch(IOException e)
            {throw e;}
        finally
        {
            if (inputStream != null)
                inputStream.close();
            if (outputStream != null)
                outputStream.close();
            bytesProcessed += file.length();
            filesProcessed++;
        }
        
        // show current stats
        if (showStatistics && filesProcessed > 0)
            if (filesProcessed % 250 == 0)
                showCurrentStats();
    }
    
    private void checkFile(File file) throws IOException, NullPointerException
    {
        if (file == null)
            throw new NullPointerException("Null pointer in file check");
        if (!file.exists())
            throw new IOException("File does not exist: " + file.getAbsolutePath());
        if (!file.canRead() || !file.canWrite())
            throw new IOException("Cannot read or write file");
    }
    
    
    public void showFile(File file) throws IOException
    {
        checkFile(file);
        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
        byte[] bytes = new byte[4096];
        
        System.out.println("File: " + file.getAbsolutePath());
        while(inputStream.read(bytes) > 0)
            for(byte b : bytes)
                System.out.print(b);
    }
        
    public final void showCurrentStats()
    {
        long duration = new Date().getTime() - startTime.getTime();
        System.out.println("Bytes processed: " + bytesProcessed);
        System.out.println("Files processed : " + filesProcessed);

        double kBytesProcessed = bytesProcessed / 1024;
        double mBytesProcessed = kBytesProcessed / 1024;
//        double kBytesProcessedPerMS = kBytesProcessed / duration;
        double durationInSeconds = duration / 1000;
        double mBytesProcessedPerSecond = mBytesProcessed / durationInSeconds;

//        System.out.println("Kbytes processed per MS : " + kBytesProcessedPerMS);
        System.out.println("Mbytes processed per second : " + mBytesProcessedPerSecond);
        
    }
    
    /**
     * @param args directory to encrypt.
     */
    public static void main(String[] args) {
        String path = "C:\\TEMP";
        DirectoryEncryption de = new DirectoryEncryption();
        
        try
        {
            de.encryptDirectory(new File(path));
        }
        catch(Exception e)
        {e.printStackTrace();}
        Date stopTime = new Date();
        
        if (showStatistics)
        {
            long duration = stopTime.getTime() - de.startTime.getTime();
            System.out.println("Run time: " + duration + " MS");
        }
        
    }
}