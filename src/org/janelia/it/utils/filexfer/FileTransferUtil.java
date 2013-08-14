/*
 * Copyright (c) 2013 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This utility supports file transfer and file digest calculation/validation
 * using configurable algorithms and resources.  To optimize performance,
 * the utility uses @link{java.nio.FileChannel} components for reading
 * and writing files.
 * <p>
 * Each utility instance tracks simple performance statistics for the last
 * operation performed.  This tracking has minimal overhead and is done
 * to facilitate basic performance analysis (typically recorded in logs).
 * <p>
 * Please note that although instances can be reused for multiple transfer
 * operations, this class is not thread safe and instances should not be
 * shared across multiple threads.
 *
 * @author Eric Trautman
 */
public class FileTransferUtil {

    /**
     * Minimum buffer size allowed is 1024 bytes (1Kb).
     */
    public static final int MIN_BUFFER_SIZE = 1024;

    /**
     * Magic transfer amount for working around a 'bug' with channel transfers
     * of large files on Windows.  See
     * <a href="http://forum.java.sun.com/thread.jspa?threadID=439695&messageID=2917510>
     * this post</a> for details.
     */
    public static final int MAX_TRANSFER_COUNT =
            (64 * 1024 * 1024) - (32 * 1024);

    /**
     * Number of milliseconds to wait between digest calculation retry attempts.
     */
    public static final long DIGEST_CALCULATION_RETRY_WAIT = 1000;

    private int bufferSize;
    private String digestAlgorithm;
    private MessageDigest digest;
    private DigestBytes digestBytes;
    private FileTransferStats stats;

    /**
     * Constructs a utility instance.
     *
     * @param  bufferSize       size of transfer buffer.
     *
     * @param  digestAlgorithm  name of digest algorithm.
     *                          If null or {@link DigestAlgorithms#NONE},
     *                          no digest will be calculated for transfers.
     *
     * @throws IllegalArgumentException
     *   if the buffer size is smaller than {@link #MIN_BUFFER_SIZE}.
     *
     * @throws NoSuchAlgorithmException
     *   if an invalid digest algorithm is specified.
     */
    public FileTransferUtil(int bufferSize,
                            String digestAlgorithm)
            throws IllegalArgumentException, NoSuchAlgorithmException {

        if (bufferSize < MIN_BUFFER_SIZE) {
            throw new IllegalArgumentException(
                    "Invalid buffer size (" + bufferSize +
                    " bytes) specified.  Values must be greater than " +
                    MIN_BUFFER_SIZE + " bytes.");
        }

        this.bufferSize = bufferSize;
        this.digestAlgorithm = digestAlgorithm;
        this.digest = DigestAlgorithms.getMessageDigest(digestAlgorithm);
        this.stats = null;
    }

    /**
     * Copies fromFile to toFile, logging completion statistics.
     * If validation is requested, the digests of both files are compared after
     * the copy completes.  If the digests do not match, the toFile is
     * removed and an exception is thrown.
     *
     * @param  fromFile      source file to copy.
     *
     * @param  toFile        target file for copy.
     *
     * @param  validateCopy  indicates whether toFile digest should be
     *                       checked after copy.
     *
     * @throws IOException
     *   if validation was requested and the fromFile and toFile digests
     *   do not match after completing the copy.  This exception will also
     *   be thrown if any other errors occur during the copy.
     */
    public void copyAndValidate(File fromFile,
                                File toFile,
                                boolean validateCopy)
            throws IOException {

        if (LOG.isInfoEnabled()) {
            LOG.info(getCopyStartMessage(fromFile, toFile));
        }

        final DigestBytes fromDigest = copy(fromFile, toFile);
        final FileTransferStats copyStats = stats;

        if (validateCopy && (fromDigest != null)) {
            final DigestBytes toDigest = calculateDigest(toFile);
            if (! fromDigest.equals(toDigest)) {
                deleteInvalidCopyAndThrowException(fromFile,
                                                   toFile,
                                                   fromDigest);
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(getSuccessfulCopyMessage(fromFile,
                                              toFile,
                                              copyStats,
                                              validateCopy));
        }
    }

    /**
     * Copies fromFile to toFile.
     *
     * @param  fromFile      source file to copy.
     *
     * @param  toFile        target file for copy.
     *
     * @return the calculated digest for the fromFile or null if
     *         this instance does not have a digest algorithm.

     * @throws IOException
     *   if any errors occur during the copy.
     */
    public DigestBytes copy(File fromFile,
                            File toFile)
            throws IOException {

        long startTime = System.currentTimeMillis();

        long bytesProcessed = 0;

        if (! fromFile.exists()) {
            throw new IOException(fromFile.getAbsolutePath() +
                                  " does not exist");
        }

        if (toFile.exists()) {
            throw new IOException(toFile.getAbsolutePath() +
                                  " already exists");
        }

        File parent = toFile.getParentFile();
        if (parent != null) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }

        FileInputStream fromStream = null;
        FileOutputStream toStream = null;
        try {
            fromStream = new FileInputStream(fromFile);
            FileChannel fromChannel = fromStream.getChannel();
            toStream = new FileOutputStream(toFile);
            FileChannel toChannel = toStream.getChannel();

            if (digest != null) {

                digest.reset();
                digestBytes = null;

                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                while (fromChannel.read(buffer) != -1)  {
                    buffer.flip();  // prepare buffer for reading by toChannel
                    while (buffer.hasRemaining()) { // handle partial writes
                        bytesProcessed += toChannel.write(buffer);
                    }
                    buffer.rewind(); // reread for digest calculation
                    digest.update(buffer);
                    buffer.clear(); // prepare for next read
                }

                digestBytes = new DigestBytes(digest.digest());

            } else {

                // This loop works around a 'bug' with channel transfers
                // of large files on Windows.
                // See http://forum.java.sun.com/thread.jspa?threadID=439695&messageID=2917510
                // for details.
                final long size = fromChannel.size();
                long position = 0;
                while (position < size) {
                    position += fromChannel.transferTo(position,
                                                       MAX_TRANSFER_COUNT,
                                                       toChannel);
                }

                bytesProcessed = size;
            }

        } finally {
            close(fromStream);
            close(toStream);
        }

        stats = new FileTransferStats(bytesProcessed,
                                      System.currentTimeMillis() - startTime);

        return digestBytes;
    }

    /**
     * Reads and calculates the digest for the specified file using this
     * instance's digest algorithm.
     *
     * @param  file  file to read.
     *
     * @return the calculated digest for the file or null if
     *         this instance does not have a digest algorithm.

     * @throws IOException
     *   if any errors occur reading the file.
     */
    public DigestBytes calculateDigest(File file)
            throws IOException {
        return calculateDigest(file, 1);
    }

    /**
     * Reads and calculates the digest for the specified file using this
     * instance's digest algorithm.
     *
     * @param  file           file to read.
     * @param  attemptNumber  number of times calculation has been attempted
     *                        (including this attempt).
     *
     * @return the calculated digest for the file or null if
     *         this instance does not have a digest algorithm.

     * @throws IOException
     *   if any errors occur reading the file.
     */
    private DigestBytes calculateDigest(File file,
                                        int attemptNumber)
            throws IOException {

        long startTime = System.currentTimeMillis();

        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            FileChannel channel = stream.getChannel();

            if (digest != null) {

                digest.reset();
                digestBytes = null;

                ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
                while (channel.read(buffer) != -1)  {
                    buffer.flip();  // prepare buffer for reading by digest
                    digest.update(buffer);
                    buffer.clear(); // prepare for next read
                }

                digestBytes = new DigestBytes(digest.digest());
            }

            final long elapsedTime = System.currentTimeMillis() - startTime;
            stats = new FileTransferStats(channel.size(), elapsedTime);

        } catch (IOException calculationException) {
            close(stream);
            stream = null;

            if (isDigestCalculationRetryNeeded(calculationException,
                                               file,
                                               attemptNumber)) {
                calculateDigest(file,
                                (attemptNumber + 1));
            } else {
                throw calculationException;
            }

        } finally {
            close(stream);
        }

        return digestBytes;
    }

    /**
     * Checks the specified attempt number to determine whether digest
     * calculation should be retried.  If a retry is needed, information
     * about the previous failure is logged and the current thread is
     * paused for 1 second before returning control to the caller.
     * This method is defined here so that it can be shared by both
     * transfer utilities.
     *
     * @param  calculationException  exception that caused the previous
     *                               calculation to fail.
     * @param  file                  file being checked.
     * @param  attemptNumber         number of attempts already made to
     *                               calculate the digest.
     *
     * @return true if digest calculation should be retried; otherwise false.
     */
    public static boolean isDigestCalculationRetryNeeded(Exception calculationException,
                                                         File file,
                                                         int attemptNumber) {
        boolean isRetryNeeded = false;
        if (attemptNumber < 4) {

            LOG.warn("isDigestCalculationRetryNeeded: failed attempt " +
                     attemptNumber + " to calculate digest for " +
                     file.getAbsolutePath() +
                     ", will retry calculation in " +
                     DIGEST_CALCULATION_RETRY_WAIT +
                     " milliseconds", calculationException);

            try {
                Thread.sleep(DIGEST_CALCULATION_RETRY_WAIT);
            } catch (InterruptedException sleepException) {
                LOG.warn("isDigestCalculationRetryNeeded: failed to " +
                         "sleep before retrying validation, " +
                         "ignoring error", sleepException);
            }

            isRetryNeeded = true;
        }

        return isRetryNeeded;
    }

    @Override
    public String toString() {
        return "FileTransfer{" +
               "bufferSize=" + bufferSize +
               ", digestAlgorithm='" + digestAlgorithm + '\'' +
               ", digestBytes=" + digestBytes +
               '}';
    }

    private void close(Closeable closable) {
        if (closable != null) {
            try {
                closable.close();
            } catch (IOException e) {
                LOG.warn("close failed, ignoring error", e);
            }
        }
    }

    private void deleteInvalidCopyAndThrowException(File fromFile,
                                                    File toFile,
                                                    DigestBytes fromDigest)
            throws IOException {

        boolean targetRemoved = false;
        try {
            //noinspection ResultOfMethodCallIgnored
            toFile.delete();
            targetRemoved = true;
        } catch (Exception e) {
            LOG.error("failed to delete " + toFile.getAbsolutePath() +
                      " after digest mismatch found", e);
        }

        StringBuilder sb = new StringBuilder(1024);
        sb.append("Copy aborted because ");
        sb.append(digestAlgorithm);
        sb.append(" digests do not match.  Target (");
        sb.append(toFile.getAbsolutePath());
        sb.append(") digest is ");
        sb.append(digestBytes);
        sb.append(" while source (");
        sb.append(fromFile.getAbsolutePath());
        sb.append(") digest is ");
        sb.append(fromDigest);
        sb.append(".  The target file has ");
        if (! targetRemoved) {
            sb.append("NOT ");
        }
        sb.append("been removed.");

        throw new IOException(sb.toString());
    }

    private String getCopyStartMessage(File fromFile,
                                       File toFile) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Starting copy of ");
        sb.append(fromFile.getAbsolutePath());
        sb.append(" to ");
        sb.append(toFile.getAbsolutePath());
        sb.append(".");
        return sb.toString();
    }

    private String getSuccessfulCopyMessage(File fromFile,
                                            File toFile,
                                            FileTransferStats copyStats,
                                            boolean validateCopy) {
        StringBuilder sb = new StringBuilder(1024);
        sb.append("Successfully copied ");
        sb.append(fromFile.getAbsolutePath());
        sb.append(" to ");
        sb.append(toFile.getAbsolutePath());

        if (! fromFile.isDirectory()) {
            sb.append(".  A total of ");
            sb.append(copyStats.getBytesProcessed());
            sb.append(" bytes were copied in ");
            sb.append(copyStats.getDurationSeconds());
            sb.append(" seconds.  ");
        }

        if (digestBytes != null) {
            if (validateCopy) {
                sb.append("Verified ");
                sb.append(digestAlgorithm);
                sb.append(" digest ");
                sb.append(digestBytes);
                sb.append(" was calculated in ");
                sb.append(stats.getDurationSeconds());
                sb.append(" seconds.");
            } else {
                sb.append("Source ");
                sb.append(digestAlgorithm);
                sb.append(" digest is ");
                sb.append(digestBytes);
                sb.append(" .");
            }
        }

        return sb.toString();
    }

    private static final Logger LOG = Logger.getLogger(FileTransferUtil.class);

    public static void main(String[] args) {
        if (args.length > 1) {
            try {
                final String algorithm = args[0];
                final FileTransferUtil util = new FileTransferUtil(10000000,
                                                                   algorithm);
                final String fileName = args[1];
                final File file = new File(fileName);

                System.out.println("Reading " + file.getAbsolutePath());

                DigestBytes db = util.calculateDigest(file);
                System.out.println("TMOG log '" + algorithm + "' digest is: " +
                                   db.toString());
                System.out.println("Standard '" + algorithm + "' digest is: " +
                                   db.toSum());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("\n\nUSAGE: java " +
                               FileTransferUtil.class.getName() +
                               " <digest algorithm> <file name>\n\n");
        }

    }
}
