/*
 * Copyright (c) 2010 Howard Hughes Medical Institute.
 * All rights reserved.
 * Use is subject to Janelia Farm Research Campus Software Copyright 1.1
 * license terms (http://license.janelia.org/license/jfrc_copyright_1_1.html).
 */

package org.janelia.it.utils.filexfer;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This tool was used to measure and test the performance of different
 * file transfer options.
 * <h2>WARNING:</h2>
 * The tool has been committed to the source repository for reference 
 * purposes only.  Be careful reusing this tool as it was hacked together
 * and has various hardcoded assumptions.
 *
 * @author Eric Trautman
 */
public class CopyProfiler {

    private static final int KILOBYTE = 1024;
    private static final long  MEGABYTE = 1024L * 1024L;

    private String digestAlgorithm;
    private int bufferSize;
    private long lastTime;

    public CopyProfiler(String digestAlgorithm,
                        int bufferSize) {
        if ((digestAlgorithm != null) && (! "n".equals(digestAlgorithm))) {
            this.digestAlgorithm = digestAlgorithm;
        }
        this.bufferSize = bufferSize;
    }

    public void copy (File srcLocation,
                      File destLocation,
                      boolean httpValidate,
                      boolean newTransfer)
            throws Exception{


        final String fmt = "%-20s%s";
        System.out.println();
        System.out.println(String.format(fmt,
                                         "src:",
                                         srcLocation.getAbsolutePath()));
        final long srcSize = srcLocation.length();
        System.out.println(String.format(fmt,
                                         "src size:",
                                         (srcSize / MEGABYTE) + " megabytes"));

        final String urlString = "http://10.101.10.195:8080/filexfer/svoboda/";
        if (destLocation == null) {
            System.out.println(String.format(fmt,
                                             "dest:",
                                             urlString));
        } else {
            System.out.println(String.format(fmt,
                                             "dest:",
                                             destLocation.getAbsolutePath()));
            System.out.println(String.format(fmt,
                                             "httpValidate:",
                                             String.valueOf(httpValidate)));
        }
        System.out.println(String.format(fmt,
                                         "newTransfer:",
                                         String.valueOf(newTransfer)));
        System.out.println(String.format(fmt,
                                         "buffer size:",
                                         (bufferSize / KILOBYTE) + " kilobytes"));
        System.out.println(String.format(fmt,
                                         "digest algorithm:",
                                         digestAlgorithm));
        System.out.println();
        System.out.println(String.format("%-20s%-70s %10s",
                                         " ",
                                         "event",
                                         "elapsed ms"));
        System.out.println(String.format(fmt, " ", "---------------------------------------------------------------------- ----------"));

        long startTime = System.currentTimeMillis();
        lastTime = startTime;

        if (destLocation == null) {
            sendRequest(srcLocation, urlString);
        } else {
            DigestBytes digestBytes;
            FileTransferUtil util = null;
            if (newTransfer) {
                util = new FileTransferUtil(bufferSize, digestAlgorithm);
                digestBytes = newTransfer(srcLocation, destLocation, util);
            } else {
                OutputStream outStream = new BufferedOutputStream(
                        new FileOutputStream(destLocation));

                digestBytes = oldCopy(srcLocation, outStream);
                outStream.close();
            }

            if (digestBytes != null) {
                if (httpValidate) {
                    final String localValue = String.valueOf(digestBytes);
                    logTime("local digest is " + localValue);
                    String remoteValue =
                            getHttpDigestValue(srcLocation, urlString);
                    final boolean isValid = localValue.equals(remoteValue);
                    logTime("isValid is " + isValid);
                } else {
                    validateDigest(destLocation, digestBytes, util);
                }
            }
        }

        final long totalTime = System.currentTimeMillis() - startTime;
        final double totalSeconds = totalTime / 1000.0;
        final double megaBytes = srcSize / MEGABYTE;
        final double mbPerSec = megaBytes / totalSeconds;
        System.out.println();
        System.out.println(String.format(fmt,
                                         "total time:",
                                         totalSeconds + " seconds"));
        System.out.println(String.format(fmt,
                                         "transfer rate:",
                                         mbPerSec + " megabytes/second"));
        System.out.println();
    }

    public DigestBytes newTransfer(File fromFile,
                              File toFile,
                              FileTransferUtil util)
        throws IOException, NoSuchAlgorithmException {

        logTime("start copy");

        DigestBytes digestBytes = util.copy(fromFile, toFile);

        logTime("finish copy");

        return digestBytes;
    }

    public DigestBytes oldCopy(File srcLocation,
                               OutputStream outStream)
        throws IOException, NoSuchAlgorithmException {

        MessageDigest digest =
                DigestAlgorithms.getMessageDigest(digestAlgorithm);

        logTime("start copy");

        InputStream inStream = new BufferedInputStream(
                new FileInputStream(srcLocation));

        if (digest != null) {
            inStream = new DigestInputStream(inStream, digest);
        }

        byte[] buffer = new byte[bufferSize];
        int rtnBytes = bufferSize;
        while (rtnBytes > 0) {
            rtnBytes = inStream.read(buffer);
            if (rtnBytes > 0) {
                outStream.write(buffer, 0, rtnBytes);
            }
        }

        inStream.close();

        logTime("finish copy");

        DigestBytes digestBytes = null;
        if (digest != null) {
            digestBytes = new DigestBytes(digest.digest());
        }

        return digestBytes;
    }

    private boolean validateDigest(File srcLocation,
                                   DigestBytes copyDigestBytes,
                                   FileTransferUtil util)
        throws NoSuchAlgorithmException,IOException {

        logTime("start validation read");

        DigestBytes validationDigestBytes;
        if (util != null) {
            validationDigestBytes = util.calculateDigest(srcLocation);
        } else {
            MessageDigest digest =
                    DigestAlgorithms.getMessageDigest(digestAlgorithm);


            InputStream inStream =  new DigestInputStream(
                    new BufferedInputStream(new FileInputStream(srcLocation)),
                    digest);

            byte[] buffer = new byte[bufferSize];
            int rtnBytes = bufferSize;
            while (rtnBytes > 0) {
                rtnBytes = inStream.read(buffer);
            }
            inStream.close();

            validationDigestBytes = new DigestBytes(digest.digest());
        }

        logTime("finish validation read");

        final boolean isValid = copyDigestBytes.equals(validationDigestBytes);

        logTime("compare digest values, isValid is " + isValid);

        return isValid;
    }

    private void logTime(String event) {
        long currentTime = System.currentTimeMillis();
        System.out.println(String.format("%-20d%-70s %10d",
                                         currentTime,
                                         event,
                                         (currentTime - lastTime)));
        lastTime = System.currentTimeMillis();
    }

    public static void main(String[] args) {

        CopyProfiler profiler;

        boolean httpValidate = (System.getProperty("httpValidate") != null);
        boolean newTransfer = (System.getProperty("newTransfer") != null);

        if (args.length > 2) {

            final String digest = args[0];
            final int bufferSize = Integer.parseInt(args[1]);

            profiler = new CopyProfiler(digest, bufferSize * KILOBYTE);

            if (args.length > 3) {
                for (int i = 2; i < args.length; i = i + 2) {
                    File srcLocation = new File(args[i]);
                    File destLocation = new File(args[i+1]);
                    try {
                        profiler.copy(srcLocation,
                                      destLocation,
                                      httpValidate,
                                      newTransfer);
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                }
            } else {
                File srcLocation = new File(args[2]);
                try {
                    profiler.copy(srcLocation, null, false, false);
                }
                catch (Exception ex){
                    ex.printStackTrace();
                }
            }

        } else {
            System.out.println("Syntax: java -cp . org.Profiler [n|sha-1|md2|md5|] 100 srcFile destFile");
            System.exit(1);
        }


    }

    private String sendRequest(File srcFile,
                               String urlString) throws IOException {

        String encodedResource = URLEncoder.encode(srcFile.getName(),
                                                   "UTF-8");

        PutMethod method = new PutMethod(urlString + encodedResource);

        int responseCode;
        String responseText;
        try {
            DigestFileRequestEntity requestEntity =
                    new DigestFileRequestEntity(srcFile,
                                                "application/octet-stream",
                                                this);
            method.setRequestEntity(requestEntity);
            NameValuePair[] queryParameters = new NameValuePair[2];
            queryParameters[0] = new NameValuePair("digestAlgorithm",
                                                   digestAlgorithm);
            queryParameters[1] = new NameValuePair("bufferSize",
                                                   String.valueOf(bufferSize));
            method.setQueryString(queryParameters);

            HttpClient httpClient = new HttpClient();
            responseCode = httpClient.executeMethod(method);
            logTime("received response code " + responseCode);

            responseText = method.getResponseBodyAsString();
            final DigestBytes copyDigestBytes = requestEntity.getDigestBytes();
            final String digestBytesStr = String.valueOf(copyDigestBytes);

            if (digestBytesStr.equals(responseText)) {
                logTime("valid digest " + digestBytesStr);
            } else {
                logTime("invalid digest " + digestBytesStr + " (" + responseText + ")");
            }


        } finally {
            method.releaseConnection();
        }

        return responseText;
    }

    private String getHttpDigestValue(File srcFile,
                                      String urlString) throws IOException {

        String encodedResource = URLEncoder.encode(srcFile.getName(),
                                                   "UTF-8");

        GetMethod method = new GetMethod(urlString + encodedResource);

        int responseCode;
        String responseText;
        try {
            NameValuePair[] queryParameters = new NameValuePair[2];
            queryParameters[0] = new NameValuePair("digestAlgorithm",
                                                   digestAlgorithm);
            queryParameters[1] = new NameValuePair("bufferSize",
                                                   String.valueOf(bufferSize));
            method.setQueryString(queryParameters);

            HttpClient httpClient = new HttpClient();
            responseCode = httpClient.executeMethod(method);
            logTime("received response code " + responseCode);

            responseText = method.getResponseBodyAsString();

            logTime("retrieved digest " + responseText);
        } finally {
            method.releaseConnection();
        }

        return responseText;
    }

    private class DigestFileRequestEntity implements RequestEntity {

        private File file;
        private String contentType;
        private CopyProfiler profiler;
        private DigestBytes digestBytes;

        public DigestFileRequestEntity(File file,
                                       String contentType,
                                       CopyProfiler profiler) {
            this.file = file;
            this.contentType = contentType;
            this.profiler = profiler;
        }

        public DigestBytes getDigestBytes() {
            return digestBytes;
        }

        public long getContentLength() {
            return this.file.length();
        }

        public String getContentType() {
            return this.contentType;
        }

        public boolean isRepeatable() {
            return true;
        }

        public void writeRequest(OutputStream out) throws IOException {
            try {
                digestBytes = profiler.oldCopy(file, out);
            } catch (NoSuchAlgorithmException e) {
                IOException ioe = new IOException();
                ioe.initCause(e);
                throw ioe;
            }
        }
    }

}