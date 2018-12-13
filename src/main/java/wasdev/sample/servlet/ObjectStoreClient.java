package wasdev.sample.servlet;

import java.io.InputStream;
import java.util.List;

import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.SDKGlobalConfiguration;
import com.ibm.cloud.objectstorage.auth.AWSCredentials;
import com.ibm.cloud.objectstorage.auth.AWSStaticCredentialsProvider;
import com.ibm.cloud.objectstorage.auth.BasicAWSCredentials;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.ibm.cloud.objectstorage.oauth.BasicIBMOAuthCredentials;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3ClientBuilder;
import com.ibm.cloud.objectstorage.services.s3.model.Bucket;
import com.ibm.cloud.objectstorage.services.s3.model.ListObjectsRequest;
import com.ibm.cloud.objectstorage.services.s3.model.ObjectListing;
import com.ibm.cloud.objectstorage.services.s3.model.ObjectMetadata;
import com.ibm.cloud.objectstorage.services.s3.model.S3Object;
import com.ibm.cloud.objectstorage.services.s3.model.S3ObjectSummary;

/**
 * Created by nirro@il.ibm.com on 13/12/18.
 */

public class ObjectStoreClient {
	private static AmazonS3 _cos;
	
	public ObjectStoreClient(String auth_endpoint, String api_key, String service_instance_id, String endpoint_url, String location) {
		SDKGlobalConfiguration.IAM_ENDPOINT = auth_endpoint;
		
		AWSCredentials credentials;
        if (endpoint_url.contains("objectstorage.softlayer.net")) {
            credentials = new BasicIBMOAuthCredentials(api_key, service_instance_id);
        } else {
            String access_key = api_key;
            String secret_key = service_instance_id;
            credentials = new BasicAWSCredentials(access_key, secret_key);
        }
        ClientConfiguration clientConfig = new ClientConfiguration().withRequestTimeout(5000);
        clientConfig.setUseTcpKeepAlive(true);

        _cos = AmazonS3ClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new EndpointConfiguration(endpoint_url, location)).withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfig).build();
	}
	
	// #Region - Buckets
	
	/**
     * @param bucketName
     */
    public void createBucket(String bucketName) {
        System.out.printf("Creating new bucket: %s\n", bucketName);
        _cos.createBucket(bucketName);
        System.out.printf("Bucket: %s created!\n", bucketName);
    }
    
    /**
     */
    public void listBuckets()
    {
        System.out.println("Listing buckets");
        final List<Bucket> bucketList = _cos.listBuckets();
        for (final Bucket bucket : bucketList) {
        	System.out.println(bucket.toString());
            System.out.println(bucket.getName());
        }
        System.out.println();
    }
    
    /**
     * @param bucketName
     */
    public void deleteBucket(String bucketName) {
        System.out.printf("Deleting bucket: %s\n", bucketName);
        _cos.deleteBucket(bucketName);
        System.out.printf("Bucket: %s deleted!\n", bucketName);
    }
    
    // #Region End - Buckets
    
    
    // #Region - Objects
    
    /**
     * @param bucketName
     * @param objectName
     * @param stream
     * @param contentType
     */
    public void createObject(String bucketName, String objectName, InputStream stream, ObjectMetadata metadata) {
    	System.out.printf("creating object %s under bucket: %s\n", objectName, bucketName);
	    _cos.putObject(bucketName, objectName, stream, metadata);
	    System.out.printf("object %s under bucket: %s created successfully\n", objectName, bucketName);
    }
    
    /**
     * @param bucketName
     * @param objectName
     */
    public S3Object getObject(String bucketName, String objectName) throws ObjectNotFoundException {
    	S3Object obj = _cos.getObject(bucketName, objectName);
    	if (obj == null) {
    		throw new ObjectNotFoundException(bucketName, objectName);
    	}
		return obj;
    }
    
    /**
     * @param bucketName
     */
    public void listObjects(String bucketName)
    {
        System.out.println("Listing objects in bucket " + bucketName);
        ObjectListing objectListing = _cos.listObjects(new ListObjectsRequest().withBucketName(bucketName));
        for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
        	
            System.out.println(" - " + objectSummary.getKey() + "  " + "(size = " + objectSummary.getSize() + ")");
        }
        System.out.println();
    }
    
    /**
     * @param bucketName
     * @param objectName
     */
    public void deleteObject(String bucketName, String objectName) {
    	System.out.printf("Deleting object: %s, which belongs to bucket %s\n", objectName, bucketName);
        _cos.deleteObject(bucketName, objectName);
        System.out.printf("object: %s under bucket %s deleted!\n", objectName, bucketName);
    }
    
 // #Region End - Objects
    
}
