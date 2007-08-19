package org.janelia.it.utils.filexfer;

import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This utility supports safe file transfers.
 * 
 * @author Peter Davies
 */
public class SafeFileTransfer {

    /** The logger for this class. */
    private static final Logger LOG = Logger.getLogger(SafeFileTransfer.class);
    private static final String DIGEST_ALGORITHM = "SHA-1";
    private static final int BUFFER_SIZE = 100 * 1024;

    /**
     * If srcLocation is a directory, the whole directory will be moved.  Pretty much the
     * equivelent of a recursive copy with a recursive delete after verification that the
     * copy was successful.
     * @param srcLocation   source to move
     * @param destLocation  target for move
     * @param overWriteExisting - this is applied to the destLoction only.  If set to true,
     * the destLocation will be deleted before srcLocation is moved to destLocation.  The side-
     * effect is that files from src will not merge with dest ever as this would be more of a
     * copy behavior instead of a move behavior
     * @throws FileMoveFailedException
     *   if any errors occur during the move.
     */
    public static void move (File srcLocation,
                             File destLocation,
                             boolean overWriteExisting)
            throws FileMoveFailedException{
         try {
             if (!srcLocation.exists()) throw new FileMoveFailedException("Cannot move non-existant file!");
             if (!overWriteExisting && destLocation.exists()) throw new FileMoveFailedException("Destination "+destLocation+" exists!");
             destLocation.getParentFile().mkdirs();
             if (overWriteExisting && destLocation.exists()) destLocation.delete();
             boolean success=srcLocation.renameTo(destLocation);
             if (success) {
                 LOG.info("Moved successfully");
                 return;
             }

             byte[] hashCode=recursiveCopy(srcLocation,destLocation);
             success=recursiveHashValidation(destLocation, hashCode);
             if (!success) {
                 destLocation.delete();
                 throw new FileMoveFailedException("Cannot move "+srcLocation.getPath()+" to "+
                         destLocation.getPath()+" and maintain data integrity");
             }
             else {
                 LOG.info("Copied successfully");
                 if (!recursiveDelete(srcLocation)){
                    recursiveDelete(destLocation);
                    throw new FileMoveFailedException("Copy of "+srcLocation.getPath()+" to "+
                         destLocation.getPath()+" was successful, but cannot delete src location");
                 }
                 else {
                    LOG.info("Deleted src successfully");
                 }
             }
         }
         catch (Throwable th){
             throw new FileMoveFailedException("File could not be moved",th);
         }
    }

    /**
     * If srcLocation is a directory, the whole directory will be copied. Will not merge
     * files with existing directory like standard copy.  For existing directories, set
     * overWrite to true or get exception.
     * @param srcLocation   source to copy
     * @param destLocation  target for copy
     * @param overWriteExisting - this is applied to the destLocation only.  If set to true,
     * the destLocation will be deleted before srcLocation is moved to destLocation.  The side-
     * effect is that files from src will not merge with dest ever as this would be more of a
     * copy behavior instead of a move behavior
     * @throws FileCopyFailedException
     *   if any errors occur during the copy.
     */
    public static void copy (File srcLocation,
                             File destLocation,
                             boolean overWriteExisting)
            throws FileCopyFailedException{

        if (LOG.isInfoEnabled()) {
            LOG.info("starting copy of " + srcLocation.getAbsolutePath() + 
                     " to " + destLocation.getAbsolutePath());
        }

         try {
             if (!srcLocation.exists()) throw new FileMoveFailedException("Cannot copy non-existent file!");
             if (!overWriteExisting && destLocation.exists()) throw new FileCopyFailedException("Destination "+destLocation+" exists!");
             destLocation.getParentFile().mkdirs();
             if (overWriteExisting && destLocation.exists()) destLocation.delete();
             byte[] hashCode=recursiveCopy(srcLocation,destLocation);
             boolean success=recursiveHashValidation(destLocation, hashCode);
             if (success) {
                 if (LOG.isInfoEnabled()) {
                     logCopyStats(hashCode, srcLocation, destLocation);
                 }
             } else {
                 destLocation.delete();
                 throw new FileCopyFailedException("Cannot copy "+srcLocation.getPath()+" to "+
                         destLocation.getPath()+" and maintain data integrity");
             }
         }
         catch (Throwable th){
             throw new FileCopyFailedException("File could not be copied",th);
         }
    }

    public static boolean recursiveDelete(File srcLocation)
        throws FileNotFoundException {

        boolean success=true;
        if (srcLocation.isDirectory()){
           File[] files=srcLocation.listFiles();
            for (File file : files) {
                success = success & recursiveDelete(file);
            }
            success=success&srcLocation.delete();

        }
        else {
            success=srcLocation.delete();
            return success;
        }
        return success;
    }


    private static byte[] recursiveCopy(File srcLocation,
                                        File destLocation)
        throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        recursiveCopyHelper(srcLocation,destLocation,digest);
        return digest.digest();
    }

    private static void recursiveCopyHelper(File srcLocation,
                                            File destLocation,
                                            MessageDigest digest)
        throws IOException {
        if (srcLocation.isDirectory()){
           File[] files=srcLocation.listFiles();
           File destination;
            for (File file : files) {
                destination = new File(destLocation, file.getName());
                recursiveCopyHelper(file, destination, digest);
            }
        }
        else {
            destLocation.getParentFile().mkdirs();
            InputStream inStream=new BufferedInputStream(new FileInputStream(srcLocation));
            OutputStream outStream=new BufferedOutputStream(new FileOutputStream(destLocation));
            addInputStreamToOuputStream(inStream,outStream,digest);
            inStream.close();
            outStream.close();
        }
    }

    static private boolean recursiveHashValidation(File srcLocation, byte[] hashValue)
        throws NoSuchAlgorithmException,IOException {
        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        recursiveHashValidationHelper(srcLocation,digest);
        byte[] digestBytes=digest.digest();
        if (hashValue.length != digestBytes.length) return false;
        for (int i = 0; i < hashValue.length; i++) {
            if (hashValue[i] != digestBytes[i]) return false;
        }
        return true;
    }

    static private void recursiveHashValidationHelper(File srcLocation,MessageDigest digest)
        throws IOException {
        if (srcLocation.isDirectory()){
           File[] files=srcLocation.listFiles();
            for (File file : files) {
                recursiveHashValidationHelper(file, digest);
            }
        }
        else {
            InputStream inStream=new DigestInputStream(
                    new BufferedInputStream(new FileInputStream(srcLocation)),digest);
            byte[] buffer = new byte[BUFFER_SIZE];
            int rtnBytes = BUFFER_SIZE;
            while (rtnBytes > 0) {
                rtnBytes = inStream.read(buffer);
             }
            inStream.close();
        }
    }

    static private void addInputStreamToOuputStream(InputStream inStream, OutputStream outStream,MessageDigest digest)
            throws IOException {
        if (digest != null)
            inStream = new DigestInputStream(inStream, digest);

        byte[] buffer = new byte[BUFFER_SIZE];
        int rtnBytes = BUFFER_SIZE;
        while (rtnBytes > 0) {
            rtnBytes = inStream.read(buffer);
            if (rtnBytes > 0 && outStream != null) outStream.write(buffer, 0, rtnBytes);
        }
    }

    /**
     * Utility to calculate and return the digest value for the specified file.
     *
     * @param  forFile  file to read.
     *
     * @return the file's calculated digest value.
     */
    public static byte[] getDigest(File forFile) {
        byte[] digestValue;
        DigestInputStream dis = null;
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            FileInputStream fis = new FileInputStream(forFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            dis = new DigestInputStream(bis, digest);
            byte[] buffer = new byte[BUFFER_SIZE];
            int rtnBytes = BUFFER_SIZE;
            while (rtnBytes > 0) {
                rtnBytes = dis.read(buffer);
            }
            digestValue = digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "unable to find MessageDigest for " + DIGEST_ALGORITHM +
                    " algorithm", e);
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "unable to access file " + forFile.getAbsolutePath(), e);
        } finally {
            closeInputStream(dis);
        }
        return digestValue;
    }

    private static void closeInputStream(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                LOG.error("failed to close input stream", e);
            }
        }
    }

    private static final SimpleDateFormat SDF =
            new SimpleDateFormat("yyyy-MM-dd hh:mm:ss'.0'");

    public static void main(String[] args) {
        boolean isValid = false;
        boolean isCopy = false;
        boolean isMove = false;
        boolean isDigest = false;
        File srcLocation = null;
        File destLocation = null;
        boolean overWrite = false;
        if (args.length > 1) {
            String action = args[0].toLowerCase();
            isCopy = "copy".equals(action);
            isMove = "move".equals(action);
            isDigest = "digest".equals(action);
            srcLocation = new File(args[1]);
            if (isDigest) {
                isValid = true;
            } else if ((isCopy || isMove) && (args.length > 2)) {
                destLocation = new File(args[2]);
                if (args.length > 3) {
                    overWrite = Boolean.valueOf(args[3]);
                }
                isValid = true;
            }
        }
        if (! isValid){
            System.out.println(
                    "Usages:\n" +
                    "(1) java " + SafeFileTransfer.class.getName() +
                    "<copy|move> <src file|directory> <destination> " +
                    "<true|false overwrite destination>\n" +
                    "(2) java " + SafeFileTransfer.class.getName() +
                    " digest <src file>");
            System.exit(1);
        }
        try {
            long t1=System.currentTimeMillis();
            if (isMove) {
                move(srcLocation, destLocation, overWrite);
            }
            else if (isCopy) {
                copy(srcLocation, destLocation, overWrite);
            } else if (isDigest) {
                byte[] digestValue = getDigest(srcLocation);
                StringBuilder sb = new StringBuilder(512);
                sb.append(SDF.format(new Date()));
                sb.append(" Recalculated ");
                sb.append(DIGEST_ALGORITHM);
                sb.append(" digest value for ");
                sb.append(srcLocation.getAbsolutePath());
                sb.append(" is ");
                for (byte b : digestValue) {
                    sb.append(b);
                }
                System.out.println(sb);
            }
            System.out.println("Time: "+(System.currentTimeMillis()-t1));
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private static void logCopyStats(byte[] hashCode,
                                     File srcLocation,
                                     File destLocation) {
        StringBuilder sb =
                new StringBuilder(hashCode.length + 256);
        sb.append("Successfully copied ");
        sb.append(srcLocation.getAbsolutePath());
        sb.append(" to ");
        sb.append(destLocation.getAbsolutePath());
        if (! srcLocation.isDirectory()) {
            sb.append(".  A total of ");
            sb.append(srcLocation.length());
            sb.append(" bytes were copied");
        }
        sb.append(".  Verified ");
        sb.append(DIGEST_ALGORITHM);
        sb.append(" digest is: ");
        for (byte b : hashCode) {
            sb.append(b);
        }
        LOG.info(sb.toString());
    }

//    /**
//     * Sample code to use FileChannel objects for copy.
//     */
//    private static boolean copyFileChannel(File fromFile,
//                                           File toFile)
//            throws IOException, NoSuchAlgorithmException {
//        boolean isSuccessful = false;
//        FileInputStream fromStream = null;
//        FileChannel fromChannel = null;
//        FileOutputStream toStream = null;
//        FileChannel toChannel = null;
//        byte[] fromDigest = new byte[0];
//        try {
//            fromStream = new FileInputStream(fromFile);
//            fromChannel = fromStream.getChannel();
//            fromDigest = digest(fromChannel);
//            toStream = new FileOutputStream(toFile);
//            toChannel = toStream.getChannel();
//
//            // This loop works around a 'bug' with channel transfers
//            // of large files on Windows.
//            // See http://forum.java.sun.com/thread.jspa?threadID=439695&messageID=2917510
//            // for details.
//            int maxCount = (64 * 1024 * 1024) - (32 * 1024);
//            long size = fromChannel.size();
//            long position = 0;
//            while (position < size) {
//                position += fromChannel.transferTo(position, maxCount, toChannel);
//            }
//        } finally {
//            Closeable[] streams = new Closeable[] {fromStream, toStream};
//            for (Closeable stream : streams) {
//                if (stream != null) {
//                    try {
//                        stream.close();
//                    } catch (IOException e) {
//                        LOG.error("close failed", e);
//                    }
//                }
//            }
//        }
//
//        isSuccessful = recursiveHashValidation(toFile, fromDigest);
//
//        return isSuccessful;
//    }
//
//    /**
//     * Taken from http://blogs.sun.com/andreas/entry/hashing_a_file_in_3
//     */
//    private static byte[] digest(FileChannel channel)
//            throws NoSuchAlgorithmException, IOException {
//        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY,
//                                        0,
//                                        channel.size());
//        MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
//        md.update(buffer.duplicate());
//        return md.digest();
//    }
}
