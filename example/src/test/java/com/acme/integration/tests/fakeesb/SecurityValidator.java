package com.acme.integration.tests.fakeesb;

import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.message.token.UsernameToken;
import org.apache.ws.security.validate.UsernameTokenValidator;

/**
 * A quick and dirty class to do CXF username/password validation
 */
public class SecurityValidator extends UsernameTokenValidator {

    private String username;
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void verifyPlaintextPassword(UsernameToken usernameToken, RequestData data) throws WSSecurityException {
        String username = usernameToken.getName();
        String password = usernameToken.getPassword();

        if (!this.username.equals(username) || !this.password.equals(password)) {
            throw new WSSecurityException(WSSecurityException.FAILED_AUTHENTICATION);
        }
    }
}
