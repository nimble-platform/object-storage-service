package wasdev.sample.servlet;

/**
 * Created by evgeniyh on 4/8/18.
 */

public class ObjectStoreCredentials {
    private final String apikey;
    private final String endpoints;
    private final String iam_apikey_description;
    private final String iam_apikey_name;
    private final String iam_role_crn;
    private final String iam_serviceid_crn;
    private final String resource_instance_id;

    public ObjectStoreCredentials(String apikey, String endpoints, String iam_apikey_description, String iam_apikey_name, String iam_role_crn, String iam_serviceid_crn, String resource_instance_id) {
        this.apikey = apikey;
        this.endpoints = endpoints;
        this.iam_apikey_description = iam_apikey_description;
        this.iam_apikey_name = iam_apikey_name;
        this.iam_role_crn = iam_role_crn;
        this.iam_serviceid_crn = iam_serviceid_crn;
        this.resource_instance_id = resource_instance_id;
    }

    public String getApikey() {
        return apikey;
    }

    public String getEndpoints() {
        return endpoints;
    }

    public String getIam_apikey_description() {
        return iam_apikey_description;
    }

    public String getIam_apikey_name() {
        return iam_apikey_name;
    }

    public String getIam_role_crn() {
        return iam_role_crn;
    }

    public String getIam_serviceid_crn() {
        return iam_serviceid_crn;
    }

    public String getResource_instance_id() {
        return resource_instance_id;
    }

    @Override
    public String toString() {
        return "ObjectStoreCredentials{" +
                "apikey='" + apikey + '\'' +
                ", endpoints='" + endpoints + '\'' +
                ", iam_apikey_description='" + iam_apikey_description + '\'' +
                ", iam_apikey_name='" + iam_apikey_name + '\'' +
                ", iam_role_crn='" + iam_role_crn + '\'' +
                ", iam_serviceid_crn='" + iam_serviceid_crn + '\'' +
                ", resource_instance_id='" + resource_instance_id + '\'' +
                '}';
    }
}
