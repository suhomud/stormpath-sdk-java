package com.stormpath.sdk.oauth;

/**
 * This class represents a request for Stormpath to authenticate an Account and exchange its api key and api secret for a valid OAuth 2.0 access token.
 * Using stormpath_social grant type
 *
 * @since 1.1.0
 */
public interface OAuthStormpathSocialGrantRequestAuthentication extends OAuthGrantRequestAuthentication {
    String getApiKeyId();

    String getApiKeySecret();
}
