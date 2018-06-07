package wasdev.sample.servlet;

import org.apache.log4j.Logger;
import org.openstack4j.api.OSClient.OSClientV3;
import org.openstack4j.api.client.IOSClientBuilder;
import org.openstack4j.model.common.Identifier;
import org.openstack4j.model.identity.v3.Token;
import org.openstack4j.openstack.OSFactory;

import java.util.Date;

/**
 * Created by evgeniyh on 6/7/18.
 */

public class TokensGenerator {
    private final static Logger logger = Logger.getLogger(TokensGenerator.class);

    private static long TEN_MINUTES = 10 * 60 * 1000;

    private Token token;
    private IOSClientBuilder.V3 builder;

    public TokensGenerator(ObjectStoreCredentials credentials) {
        setOSBuilder(credentials);
        OSClientV3 os = builder.authenticate();
//        os.useRegion(credentials.getRegion());
        token = os.getToken();

        new Thread(() -> {
            logger.info("Starting the tokens update thread");
            while (true) {
                try {
                    updateTokenIfNeeded();
                    Thread.sleep(TEN_MINUTES);
                } catch (Exception e) {
                    logger.error("Error during update of the tokens", e);
                }
            }
        }).start();
    }

    private void updateTokenIfNeeded() {
        Date current = new Date();
        Date currentMinusTwentyMinutes = new Date(current.getTime() - (2 * TEN_MINUTES));
        if (token.getExpires().before(currentMinusTwentyMinutes)) {
            logger.info("The token will expire in less then 10 minutes - issuing a new one");

            OSClientV3 os = builder.authenticate();
            token = os.getToken();

            logToken();
        }
    }

    private void logToken() {
        logger.info(String.format("Current token was issued on '%s' and will expire on '%s'", token.getIssuedAt(), token.getExpires()));
    }

    public Token getToken() {
        return token;
    }

    private void setOSBuilder(ObjectStoreCredentials credentials) {
        String authUrl = credentials.getAuth_url() + "/v3";
        logger.info("Authenticating against - " + authUrl);

        Identifier domainIdentifier = Identifier.byId(credentials.getDomainId());
        builder = OSFactory.builderV3()
                .endpoint(authUrl)
                .credentials(credentials.getUsername(), credentials.getPassword(), domainIdentifier)
                .scopeToProject(Identifier.byId(credentials.getProjectId()));


        logger.info("Authenticated and created client successfully!");
    }
}
