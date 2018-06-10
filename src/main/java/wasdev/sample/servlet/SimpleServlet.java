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
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.model.storage.object.options.ObjectPutOptions;
import org.openstack4j.openstack.OSFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

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

    //	Currently it is hardcoded to use one predefined container;
    private static final String CONTAINER_NAME = "test";
    private static TokensGenerator tokensGenerator;

    static {
        String credentialsJson = System.getenv("OBJECT_STORE_CREDENTIALS");
        if (credentialsJson == null) {
            throw new IllegalStateException("ERROR !!! - Missing object store credentials environment variable");
        }
        ObjectStoreCredentials credentials = (new Gson()).fromJson(credentialsJson, ObjectStoreCredentials.class);

        tokensGenerator = new TokensGenerator(credentials);
    }

    private String getFilenameFromPath(HttpServletRequest request) {
        String pathInfo = request.getPathInfo();
        String tmp = pathInfo.substring(1);
        return (tmp.isEmpty() ? null : tmp);
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectStorageService objectStorage = OSFactory.clientFromToken(tokensGenerator.getToken()).objectStorage();
        String fileName = getFilenameFromPath(request);

        if (fileName == null) { //No file was given at the path
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            logger.error("File name was not specified.");
            return;
        }
        logger.info(String.format("Retrieving file '%s' from ObjectStorage...", fileName));

        SwiftObject fileObj = objectStorage.objects().get(CONTAINER_NAME, fileName);

        if (fileObj == null) { //The specified file was not found
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            logger.error(fileName + " Wasn't found in Object Storage");
            return;
        }


        logger.info(String.format("Sending file '%s' with mime-type '%s' and size '%s' B", fileName, fileObj.getMimeType(), fileObj.getSizeInBytes()));

        response.setContentType(fileObj.getMimeType());
        response.setHeader("Content-disposition", "inline; filename=" + fileName);

        DLPayload payload = fileObj.download();

        try (InputStream in = payload.getInputStream();
             OutputStream out = response.getOutputStream()) {
            long copied = IOUtils.copy(in, out);
            logger.info(String.format("For file '%s' were copied '%d' bytes", fileName, copied));
        }

        logger.info(fileName + " was successfully retrieved from ObjectStorage");
    }

    //    TODO: maybe change to the native servlet API
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectStorageService objectStorage = OSFactory.clientFromToken(tokensGenerator.getToken()).objectStorage();
        String fileName = getFilenameFromPath(request);
        if (fileName == null) { //No file was specified to be found
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            response.getOutputStream().print("Must provide a filename");
            logger.info("Sent empty file name");
            return;
        }

        try {
            if (ServletFileUpload.isMultipartContent(request)) {
                handleMultiPart(request, objectStorage, fileName);
            } else {
                handleWithContentType(request, objectStorage, fileName);
            }

            logger.info(String.format("Successfully stored file '%s' in ObjectStorage!", fileName));
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (Exception e) {
            logger.error("Exception during store of the file - " + fileName, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    private void printHeaders(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) {
            String h = headers.nextElement();
            logger.info(String.format("%s = %s", h, request.getHeader(h)));
        }
    }

    private void handleWithContentType(HttpServletRequest request, ObjectStorageService objectStorage, String fileName) throws IOException {
        String mimeType = request.getContentType().split(";")[0];
        logger.info(String.format("Storing file '%s' with mime type '%s' in ObjectStorage...", fileName, mimeType));

        ObjectPutOptions options = ObjectPutOptions.create().contentType(mimeType);
        final InputStream fileStream = request.getInputStream();

        objectStorage.objects().put(CONTAINER_NAME, fileName, Payloads.create(fileStream), options);
    }

    private void handleMultiPart(HttpServletRequest request, ObjectStorageService objectStorage, String fileName) throws FileUploadException, IOException {
        logger.info(String.format("Storing file '%s' as part of multipart in Object Storage...", fileName));

        ServletFileUpload fileUpload = new ServletFileUpload();
        FileItemIterator items = fileUpload.getItemIterator(request);
        ObjectPutOptions options;

        while (items.hasNext()) {
            FileItemStream item = items.next();
            logger.info(String.format("Content=%s , Name=%s, field=%s", item.getContentType(), item.getName(), item.getFieldName()));
            options = ObjectPutOptions.create().contentType(item.getContentType());
            if (!item.isFormField()) {
                InputStream is = item.openStream();
                objectStorage.objects().put(CONTAINER_NAME, fileName, Payloads.create(is), options);
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectStorageService objectStorage = OSFactory.clientFromToken(tokensGenerator.getToken()).objectStorage();
        String fileName = getFilenameFromPath(request);

        logger.info(String.format("Deleting file '%s' from ObjectStorage...", fileName));

        if (fileName == null) { //No file was specified to be found, or container name is missing
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            logger.info("File not found.");
            return;
        }

        ActionResponse deleteResponse = objectStorage.objects().delete(CONTAINER_NAME, fileName);

        if (!deleteResponse.isSuccess()) {
            response.sendError(deleteResponse.getCode());
            logger.info("Delete failed: " + deleteResponse.getFault());
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            logger.info("Successfully deleted file from ObjectStorage!");
        }
    }
}
