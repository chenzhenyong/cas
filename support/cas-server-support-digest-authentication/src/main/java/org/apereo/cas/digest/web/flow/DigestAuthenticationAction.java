package org.apereo.cas.digest.web.flow;

import org.apereo.cas.authentication.Credential;
import org.apereo.cas.digest.DigestCredential;
import org.apereo.cas.digest.DigestHashedCredentialRetriever;
import org.apereo.cas.digest.util.DigestAuthenticationUtils;
import org.apereo.cas.web.flow.AbstractNonInteractiveCredentialsAction;
import org.apereo.cas.web.support.WebUtils;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.J2EContext;
import org.pac4j.core.context.WebContext;
import org.pac4j.http.credentials.DigestCredentials;
import org.pac4j.http.credentials.extractor.DigestAuthExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.webflow.execution.RequestContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is {@link DigestAuthenticationAction} that extracts digest authN credentials from the request.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
public class DigestAuthenticationAction extends AbstractNonInteractiveCredentialsAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DigestAuthenticationAction.class);

    private String nonce = DigestAuthenticationUtils.createNonce();

    private DigestHashedCredentialRetriever credentialRetriever;

    private String realm = "CAS";

    private String authenticationMethod = "auth";
    
    public void setCredentialRetriever(final DigestHashedCredentialRetriever credentialRetriever) {
        this.credentialRetriever = credentialRetriever;
    }

    public void setRealm(final String realm) {
        this.realm = realm;
    }

    public void setAuthenticationMethod(final String authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    @Override
    protected Credential constructCredentialsFromRequest(final RequestContext requestContext) {
        try {
            final HttpServletRequest request = WebUtils.getHttpServletRequest(requestContext);
            final HttpServletResponse response = WebUtils.getHttpServletResponse(requestContext);

            final DigestAuthExtractor extractor = new DigestAuthExtractor(this.getClass().getSimpleName());
            final WebContext webContext = new J2EContext(request, response);

            final DigestCredentials credentials = extractor.extract(webContext);
            if (credentials == null) {
                response.addHeader(HttpConstants.AUTHENTICATE_HEADER,
                        DigestAuthenticationUtils.createAuthenticateHeader(this.realm, this.authenticationMethod, this.nonce));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }

            LOGGER.debug("Received digest authentication request from credentials {} ", credentials);
            final String serverResponse = credentials.calculateServerDigest(true,
                    this.credentialRetriever.findCredential(credentials.getUsername(), this.realm));

            final String clientResponse = credentials.getToken();
            if (!serverResponse.equals(clientResponse)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return null;
            }

            return new DigestCredential(credentials.getUsername(), this.realm, credentials.getToken());

        } catch (final Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
