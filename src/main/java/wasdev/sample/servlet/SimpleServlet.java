/**
 * Copyright 2015, 2016 IBM Corp. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wasdev.sample.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

/**
 * This servlet implements the /objectStorage endpoint that supports GET, POST and DELETE 
 * in order to retrieve, add/update and delete files in Object Storage, respectively.
 */
@WebServlet("/objectStorage")
public class SimpleServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	//Get these credentials from Bluemix by going to your Object Storage service, and clicking on Service Credentials:
	private static String USERNAME;
	private static String PASSWORD;
	private static String DOMAIN_ID;
	private static String PROJECT_ID;
	private static String OBJECT_STORAGE_AUTH_URL;
//	private final String OBJECT_STORAGE_AUTH_URL = "https://lon-identity.open.softlayer.com/v3";

//	Currently it is hardcoded to use one predefined container;
	private static final String CONTAINER_NAME = "test";
	private static Token token;

	public SimpleServlet(){
		System.out.println("Inside the class constructor - initializing vcap variables");
		String vcapServices = System.getenv("VCAP_SERVICES");
		if (vcapServices == null) {
			throw new IllegalStateException("ERROR !!! - vcap services is null the service is running locally or not bind to object store service");
		}
		JsonObject jsonObject = (JsonObject) (new JsonParser().parse(vcapServices));
		JsonArray osArray = jsonObject.getAsJsonArray("Object-Storage");
		JsonObject credentials = osArray.get(0).getAsJsonObject().get("credentials").getAsJsonObject();
		USERNAME = credentials.get("username").getAsString();
		PASSWORD = credentials.get("password").getAsString();
		DOMAIN_ID = credentials.get("domainId").getAsString();
		PROJECT_ID = credentials.get("projectId").getAsString();
		OBJECT_STORAGE_AUTH_URL = credentials.get("auth_url").getAsString() + "/v3";

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

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		ObjectStorageService objectStorage = authenticateAndGetObjectStorageService();
//		System.out.println("The token is " + token.toString());
		ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();

		String fileName = request.getParameter("file");
		
		System.out.println(String.format("Retrieving file '%s' from ObjectStorage...", fileName));

		if(fileName == null){ //No file was specified to be found, or container name is missing
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			System.out.println("File name was not specified.");
			return;
		}

		SwiftObject pictureObj = objectStorage.objects().get(CONTAINER_NAME, fileName);

		if (pictureObj == null){ //The specified file was not found
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			System.out.println("File not found.");
			return;
		}

		String mimeType = pictureObj.getMimeType();
        response.setContentType(mimeType);

		DLPayload payload = pictureObj.download();

		try (InputStream in = payload.getInputStream();
				OutputStream out = response.getOutputStream()) {
			IOUtils.copy(in, out);
        }

		System.out.println("Successfully retrieved file from ObjectStorage!");
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//		ObjectStorageService objectStorage = authenticateAndGetObjectStorageService();
		ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();

		String fileName = request.getParameter("file");
        
		System.out.println(String.format("Storing file '%s' in ObjectStorage...", fileName));

		if (fileName == null){ //No file was specified to be found, or container name is missing
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
		private InputStream stream = null;

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
//		ObjectStorageService objectStorage = authenticateAndGetObjectStorageService();
		ObjectStorageService objectStorage = OSFactory.clientFromToken(token).objectStorage();

		String fileName = request.getParameter("file");

		System.out.println(String.format("Deleting file '%s' from ObjectStorage...", fileName));

		if (fileName == null){ //No file was specified to be found, or container name is missing
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			System.out.println("File not found.");
			return;
		}
        
		ActionResponse deleteResponse = objectStorage.objects().delete(CONTAINER_NAME, fileName);

		if(!deleteResponse.isSuccess()){
			response.sendError(deleteResponse.getCode());
			System.out.println("Delete failed: " + deleteResponse.getFault());
		} else {
			response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("Successfully deleted file from ObjectStorage!");
        }
	}

}
