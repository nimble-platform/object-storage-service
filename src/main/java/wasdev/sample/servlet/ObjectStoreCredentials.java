package wasdev.sample.servlet;

/**
 * Created by nirro@il.ibm.com on 13/12/18.
 */

public class ObjectStoreCredentials {
	private final String auth_endpoint;
	private final String apikey;
    private final String resource_instance_id;
    private final String endpoint_url;
    private final String endpoint_location;
    private final String bucket_name;

    public ObjectStoreCredentials(String auth_endpoint, String apikey, String resource_instance_id, String endpoint_url, String endpoint_location, String bucket_name) {
        this.auth_endpoint = auth_endpoint;
    	this.apikey = apikey;
        this.resource_instance_id = resource_instance_id;
        this.endpoint_url = endpoint_url;
        this.endpoint_location = endpoint_location;
        this.bucket_name = bucket_name;
    }

    public String getAuthEndpoint() {
    	return auth_endpoint;
    }
    
    public String getApikey() {
        return apikey;
    }

    public String getResource_instance_id() {
        return resource_instance_id;
    }
    
    public String getEndpointUrl() {
    	return endpoint_url;
    }
    
    public String getEndpointLocation() {
    	return endpoint_location;
    }
    
    public String getBucketName() {
    	return bucket_name;
    }

    @Override
    public String toString() {
        return "ObjectStoreCredentials{" +
                "apikey='" + apikey + '\'' +
                ", resource_instance_id='" + resource_instance_id + '\'' +
                ", endpoint_url='" + endpoint_url + '\'' +
                ", endpoint_location='" + endpoint_location + '\'' +
                ", auth_endpoint='" + auth_endpoint + '\'' +
                ", bucket_name='" + bucket_name + '\'' +
                '}';
    }
}
