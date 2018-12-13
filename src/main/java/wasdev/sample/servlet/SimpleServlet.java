/**
 * Copyright 2015, 2016 IBM Corp. All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wasdev.sample.servlet;

import com.google.gson.Gson;
import com.ibm.cloud.objectstorage.services.s3.model.ObjectMetadata;
import com.ibm.cloud.objectstorage.services.s3.model.S3Object;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This servlet implements the /objectStorage endpoint that supports GET, POST and DELETE
 * in order to retrieve, add/update and delete files in Object Storage, respectively.
 */
@WebServlet(
        urlPatterns = "/*",
        loadOnStartup = 1)
public class SimpleServlet extends HttpServlet {
    private final static Logger logger = Logger.getLogger(SimpleServlet.class);

    private static final long serialVersionUID = 1L;

    private static final ObjectStoreCredentials creds;
    private static final ObjectStoreClient _objStoreClient;
    private static final String _bucket_name;

    static {
    	logger.info("trying to initialize servlet with obj store credentials");
        String credentialsJson = System.getenv("OBJECT_STORE_CREDENTIALS");
        if (isNullOrEmpty(credentialsJson)) {
            throw new IllegalStateException("ERROR !!! - Missing object store credentials environment variable");
        }
        creds = (new Gson()).fromJson(credentialsJson, ObjectStoreCredentials.class);
        logger.info("The new aws credentials have been set");
        
        _bucket_name = creds.getBucketName();
        _objStoreClient = new ObjectStoreClient(creds.getAuthEndpoint(), creds.getApikey(), creds.getResource_instance_id(), creds.getEndpointUrl(), creds.getEndpointLocation());
    }   

    private String getFilenameFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String tmp = pathInfo.substring(1);
        return (tmp.isEmpty() ? null : tmp);
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        ObjectStorageService objectStorage = OSFactory.clientFromToken(tokensGenerator.getToken()).objectStorage();
        String fileName = getFilenameFromPath(request);
        if (isNullOrEmpty(fileName)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            logger.error("File name was not specified.");
            return;
        }
        
        logger.info(String.format("Retrieving file '%s' from ObjectStorage...", fileName));
        
        S3Object fileObj;
        try {
        	fileObj = _objStoreClient.getObject(_bucket_name, fileName);
        }
        catch (ObjectNotFoundException ex) {
        	response.sendError(HttpServletResponse.SC_NOT_FOUND);
            logger.error(fileName + " Wasn't found in Object Storage");
            return;
        }

        long fileSize = fileObj.getObjectMetadata().getContentLength();
        logger.info(String.format("Sending file '%s' with mime-type '%s' and size '%s' B", fileName, fileObj.getObjectMetadata().getContentEncoding(), fileSize));

        response.setContentType(fileObj.getObjectMetadata().getContentEncoding());
        response.setHeader("Content-disposition", "inline; filename=" + fileName);

        try (InputStream in = fileObj.getObjectContent();
             OutputStream out = response.getOutputStream()) {
            long copied = IOUtils.copy(in, out);
            if (copied != fileSize) {
                throw new RuntimeException("Failed to read all the bytes for file - " + fileName);
            }
            logger.info(String.format("File '%s' was successfully sent with size '%d' bytes", fileName, copied));
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            logger.error("Error during read of file - " + fileName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleWithContentType(HttpServletRequest request, String fileName) throws IOException {
        String mimeType = request.getContentType().split(";")[0];
        logger.info(String.format("Storing file '%s' with mime type '%s' in ObjectStorage...", fileName, mimeType));

        final InputStream fileStream = request.getInputStream();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentEncoding(mimeType);
        _objStoreClient.createObject(_bucket_name, fileName, fileStream, metadata);
    }

    private void handleMultiPart(HttpServletRequest request, String fileName) throws FileUploadException, IOException {
        logger.info(String.format("Storing file '%s' as part of multipart in Object Storage...", fileName));

        ServletFileUpload fileUpload = new ServletFileUpload();
        FileItemIterator items = fileUpload.getItemIterator(request);
        while (items.hasNext()) {
            FileItemStream item = items.next();
            logger.info(String.format("Content=%s , Name=%s, field=%s", item.getContentType(), item.getName(), item.getFieldName()));
            if (!item.isFormField()) {
                InputStream stream = item.openStream();
                ObjectMetadata metadata = new ObjectMetadata(); 
        	    metadata.setContentType(item.getContentType()); 
                _objStoreClient.createObject(_bucket_name, fileName, stream, metadata);
            }
        }
    }

    //    TODO: maybe change to the native servlet API
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileName = getFilenameFromPath(request);
        if (isNullOrEmpty(fileName)) { //No file was specified
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            response.getOutputStream().print("Must provide a filename");
            logger.info("Sent empty file name");
            return;
        }

        try {
            if (ServletFileUpload.isMultipartContent(request)) {
                handleMultiPart(request, fileName);
            } else {
                handleWithContentType(request, fileName);
            }

            logger.info(String.format("Successfully stored file '%s' in ObjectStorage!", fileName));
            response.setStatus(HttpServletResponse.SC_CREATED);
        } catch (Exception e) {
            logger.error("Exception during store of the file - " + fileName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String fileName = getFilenameFromPath(request);

        if (isNullOrEmpty(fileName)) { //No file was specified to be found, or container name is missing
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            logger.info("File not found.");
            return;
        }

        logger.info(String.format("Deleting file '%s' from ObjectStorage...", fileName));
        try {
            _objStoreClient.deleteObject(_bucket_name, fileName);

            logger.info("Successfully deleted file from ObjectStorage!");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            logger.error("Error during deletion");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
