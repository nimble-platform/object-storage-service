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
import org.openstack4j.model.common.Payloads;
import org.openstack4j.model.identity.v3.Token;
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

/**
 * This servlet implements the /objectStorage endpoint that supports GET, POST and DELETE
 * in order to retrieve, add/update and delete files in Object Storage, respectively.
 */
@WebServlet(
        urlPatterns = "/*",
        loadOnStartup = 1)
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    //	Currently it is hardcoded to use one predefined container;
    private static final String CONTAINER_NAME = "test";
    private static Token token;

    static {
        String credentialsJson = System.getenv("OBJECT_STORE_CREDENTIALS");
        if (credentialsJson == null) {
            throw new IllegalStateException("ERROR !!! - Missing object store credentials environment variable");
        }
        ObjectStoreCredentials credentials = (new Gson()).fromJson(credentialsJson, ObjectStoreCredentials.class);
        token = getAccessToken(credentials);

        System.out.println("Successfully set up the authentication variables");
    }

    private static Token getAccessToken(ObjectStoreCredentials creds) {
        Identifier domainIdentifier = Identifier.byId(creds.getDomainId());

        String authUrl = creds.getAuth_url() + "/v3";
        System.out.println("Authenticating against - " + authUrl);

        OSClientV3 os = OSFactory.builderV3()
                .endpoint(authUrl)
                .credentials(creds.getUsername(), creds.getPassword(), domainIdentifier)
                .scopeToProject(Identifier.byId(creds.getProjectId()))
                .authenticate();

        System.out.println("Authenticated successfully! - Creating token");
        os.useRegion(creds.getRegion());

        return os.getToken();
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
//      TODO: add uje region after the client is created os.useRegion(creds.getRegion());
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

        String mimeType = fileObj.getMimeType();
        System.out.println("Mime type = " + mimeType);

        response.setContentType(mimeType);

//        response.setContentType("application/x-msdownload");
        response.setHeader("Content-disposition", "inline; filename=" + fileName);

        DLPayload payload = fileObj.download();

        try (InputStream in = payload.getInputStream();
             OutputStream out = response.getOutputStream()) {
            IOUtils.copy(in, out);
        }

        System.out.println("Successfully retrieved file from ObjectStorage!");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();

        String fileName = getFilenameFromPath(request);
        if (fileName == null) { //No file was specified to be found
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            System.out.println("File not found.");
            return;
        }

        String mimeType = request.getHeader("Content-Type");
        ObjectPutOptions options = ObjectPutOptions.create().contentType(mimeType);

        System.out.println(String.format("Storing file '%s' with mime type '%s' in ObjectStorage...", fileName, mimeType));

        final InputStream fileStream = request.getInputStream();

        objectStorage.objects().put(CONTAINER_NAME, fileName, Payloads.create(fileStream), options);

        System.out.println("Successfully stored file in ObjectStorage!");
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
