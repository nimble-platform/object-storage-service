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
import org.apache.commons.io.IOUtils;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.storage.ObjectStorageService;
import org.openstack4j.model.common.ActionResponse;
import org.openstack4j.model.common.DLPayload;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.common.Payload;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.model.storage.object.SwiftObject;
import org.openstack4j.openstack.OSFactory;

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
    private static final long serialVersionUID = 1L;

    //Get these credentials from Bluemix by going to your Object Storage service, and clicking on Service Credentials:
    private static String USERNAME;
    private static String PASSWORD;
    private static String DOMAIN_ID;
    private static String PROJECT_ID;
    private static String OBJECT_STORAGE_AUTH_URL;

    //	Currently it is hardcoded to use one predefined container;
    private static final String CONTAINER_NAME = "test";
    private static Token token;

    static {
        String credentialsJson = System.getenv("OBJECT_STORE_CREDENTIALS");
        if (credentialsJson == null) {
            throw new IllegalStateException("ERROR !!! - Missing object store credentials environment variable");
        }
        ObjectStoreCredentials credentials = (new Gson()).fromJson(credentialsJson, ObjectStoreCredentials.class);

        USERNAME = credentials.getUsername();
        PASSWORD = credentials.getPassword();
        DOMAIN_ID = credentials.getDomainId();
        PROJECT_ID = credentials.getProjectId();
        OBJECT_STORAGE_AUTH_URL = credentials.getAuth_url() + "/v3";

        token = getAccessToken();

        System.out.println("Successfully set up the authentication variables");
    }

    private static Token getAccessToken() {
        Identifier domainIdentifier = Identifier.byId(DOMAIN_ID);

        System.out.println("Authenticating against - " + OBJECT_STORAGE_AUTH_URL);

        OSClientV3 os = OSFactory.builderV3()
                .endpoint(OBJECT_STORAGE_AUTH_URL)
                .credentials(USERNAME, PASSWORD, domainIdentifier)
                .scopeToProject(Identifier.byId(PROJECT_ID))
                .authenticate();

        System.out.println("Authenticated successfully!");

        System.out.println("Creating token");
        return os.getToken();
    }

//	private ObjectStorageService authenticateAndGetObjectStorageService() {
//
//		Identifier domainIdentifier = Identifier.byId(DOMAIN_ID);
//
//		System.out.println("Authenticating against - " + OBJECT_STORAGE_AUTH_URL);
//
//		OSClientV3 os = OSFactory.builderV3()
//				.endpoint(OBJECT_STORAGE_AUTH_URL)
//				.credentials(USERNAME, PASSWORD, domainIdentifier)
//				.scopeToProject(Identifier.byId(PROJECT_ID))
//				.authenticate();
//
//		System.out.println("Authenticated successfully!");
//
//		return os.objectStorage();
//	}

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
        ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();
        String fileName = getFilenameFromPath(request);

        System.out.println(String.format("Retrieving file '%s' from ObjectStorage...", fileName));

        if (fileName == null) { //No file was given at the path
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("File name was not specified.");
            return;
        }

        SwiftObject fileObj = objectStorage.objects().get(CONTAINER_NAME, fileName);

        if (fileObj == null) { //The specified file was not found
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("File not found.");
            return;
        }

//		String mimeType = fileObj.getMimeType();
//		System.out.println("Mime type = " + mimeType);

        response.setContentType("application/x-msdownload");
        response.setHeader("Content-disposition", "attachment; filename=" + fileName);

        DLPayload payload = fileObj.download();

        try (InputStream in = payload.getInputStream();
             OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
        }

        System.out.println("Successfully retrieved file from ObjectStorage!");
//        Response.ok(new StreamingOutput() {
//            @Override
//            public void write(OutputStream output) throws IOException, WebApplicationException {
//
//            }
//        });
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();

        String fileName = getFilenameFromPath(request);

        System.out.println(String.format("Storing file '%s' in ObjectStorage...", fileName));

        if (fileName == null) { //No file was specified to be found, or container name is missing
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("File not found.");
            return;
        }

        final InputStream fileStream = request.getInputStream();

        Payload<InputStream> payload = new PayloadClass(fileStream);
        objectStorage.objects().put(CONTAINER_NAME, fileName, payload);

        System.out.println("Successfully stored file in ObjectStorage!");
    }

    private class PayloadClass implements Payload<InputStream> {
        private InputStream stream;

        PayloadClass(InputStream stream) {
            this.stream = stream;
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }

        @Override
        public InputStream open() {
            return stream;
        }

        @Override
        public void closeQuietly() {
            try {
                stream.close();
            } catch (IOException e) {
            }
        }

        @Override
        public InputStream getRaw() {
            return stream;
        }

    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();
        String fileName = getFilenameFromPath(request);

        System.out.println(String.format("Deleting file '%s' from ObjectStorage...", fileName));

        if (fileName == null) { //No file was specified to be found, or container name is missing
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("File not found.");
            return;
        }

        ActionResponse deleteResponse = objectStorage.objects().delete(CONTAINER_NAME, fileName);

        if (!deleteResponse.isSuccess()) {
            response.sendError(deleteResponse.getCode());
            System.out.println("Delete failed: " + deleteResponse.getFault());
        } else {
            response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("Successfully deleted file from ObjectStorage!");
        }
    }

}
