/*
 * Copyright (c) 2012 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

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
    public static final String DIGEST_ALGORITHM = "md5";
    public static final int BUFFER_SIZE = 1024 * 1024;

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
             final long copyStartTime = System.currentTimeMillis();
             byte[] hashCode=recursiveCopy(srcLocation,destLocation);
             final long valStartTime = System.currentTimeMillis();
             boolean success=recursiveHashValidation(destLocation, hashCode);
             final long valStopTime = System.currentTimeMillis();
             if (success) {
                 if (LOG.isInfoEnabled()) {
                     final double copyDurationSeconds =
                             (valStartTime - copyStartTime) / 1000.0;
                     final double valDurationSeconds =
                             (valStopTime - valStartTime) / 1000.0;
                     logCopyStats(hashCode, srcLocation, destLocation,
                                  copyDurationSeconds, valDurationSeconds);
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
        recursiveHashValidationHelper(srcLocation, digest, 1);
        byte[] digestBytes=digest.digest();
        if (hashValue.length != digestBytes.length) return false;
        for (int i = 0; i < hashValue.length; i++) {
            if (hashValue[i] != digestBytes[i]) return false;
        }
        return true;
    }

    static protected void recursiveHashValidationHelper(File srcLocation,
                                                        MessageDigest digest,
                                                        int attemptNumber)
            throws IOException {

        if (srcLocation.isDirectory()) {
            final File[] files = srcLocation.listFiles();
            if (files != null) {
                for (File file : files) {
                    recursiveHashValidationHelper(file, digest, attemptNumber);
                }
            }
        } else {
            InputStream inStream = null;
            try {
                inStream =
                        new DigestInputStream(
                                new BufferedInputStream(
                                        new FileInputStream(srcLocation)),
                                digest);
                byte[] buffer = new byte[BUFFER_SIZE];
                int rtnBytes = BUFFER_SIZE;
                while (rtnBytes > 0) {
                    rtnBytes = inStream.read(buffer);
                }
            } catch (IOException calculationException) {

                closeInputStream(inStream);
                inStream = null;

                if (FileTransferUtil.isDigestCalculationRetryNeeded(calculationException,
                                                                    srcLocation,
                                                                    attemptNumber)) {
                    recursiveHashValidationHelper(srcLocation,
                                                  digest,
                                                  (attemptNumber + 1));
                } else {
                    throw calculationException;
                }

            } finally {
                closeInputStream(inStream);
            }
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
                                     File destLocation,
                                     double copyDurationSeconds,
                                     double validationDurationSeconds) {
        StringBuilder sb =
                new StringBuilder(hashCode.length + 256);
        sb.append("Successfully copied ");
        sb.append(srcLocation.getAbsolutePath());
        sb.append(" to ");
        sb.append(destLocation.getAbsolutePath());
        if (! srcLocation.isDirectory()) {
            sb.append(".  A total of ");
            sb.append(srcLocation.length());
            sb.append(" bytes were copied in ");
            sb.append(copyDurationSeconds);
            sb.append(" seconds");
        }
        sb.append(".  Verified ");
        sb.append(DIGEST_ALGORITHM);
        sb.append(" digest ");
        for (byte b : hashCode) {
            sb.append(b);
        }
        sb.append(" was calculated in ");
        sb.append(validationDurationSeconds);
        sb.append(" seconds.");
        LOG.info(sb.toString());
    }
}
