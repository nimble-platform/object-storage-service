package wasdev.sample.servlet;

/**
 * Created by evgeniyh on 4/8/18.
 */

public class ObjectStoreCredentials {
    private final String auth_url;
    private final String project;
    private final String projectId;
    private final String region;
    private final String userId;
    private final String username;
    private final String password;
    private final String domainId;
    private final String domainName;
    private final String role;

    public ObjectStoreCredentials(String auth_url, String project, String projectId, String region, String userId, String username, String password , String domainId, String domainName, String role) {
        this.auth_url = auth_url;
        this.project = project;
        this.projectId = projectId;
        this.region = region;
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.domainId = domainId;
        this.domainName = domainName;
        this.role = role;
    }

    public String getAuth_url() {
        return auth_url;
    }

    public String getProject() {
        return project;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getRegion() {
        return region;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDomainId() {
        return domainId;
    }

    public String getDomainName() {
        return domainName;
    }

    public String getRole() {
        return role;
    }
}
