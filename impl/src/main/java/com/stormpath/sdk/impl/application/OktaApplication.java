package com.stormpath.sdk.impl.application;

import com.stormpath.sdk.account.Account;
import com.stormpath.sdk.account.AccountCriteria;
import com.stormpath.sdk.account.AccountLinkingPolicy;
import com.stormpath.sdk.account.AccountList;
import com.stormpath.sdk.account.CreateAccountRequest;
import com.stormpath.sdk.account.PasswordResetToken;
import com.stormpath.sdk.account.VerificationEmailRequest;
import com.stormpath.sdk.api.ApiKey;
import com.stormpath.sdk.api.ApiKeyOptions;
import com.stormpath.sdk.application.Application;
import com.stormpath.sdk.application.ApplicationAccountStoreMapping;
import com.stormpath.sdk.application.ApplicationAccountStoreMappingCriteria;
import com.stormpath.sdk.application.ApplicationAccountStoreMappingList;
import com.stormpath.sdk.application.ApplicationOptions;
import com.stormpath.sdk.application.ApplicationStatus;
import com.stormpath.sdk.application.OAuthApplication;
import com.stormpath.sdk.impl.oauth.OktaOAuthPasswordGrantRequestAuthenticator;
import com.stormpath.sdk.impl.oauth.OktaOAuthRefreshTokenRequestAuthenticator;
import com.stormpath.sdk.impl.oauth.OktaOAuthRequestAuthenticator;
import com.stormpath.sdk.okta.OktaForgotPasswordRequest;
import com.stormpath.sdk.okta.OktaForgotPasswordResult;
import com.stormpath.sdk.okta.OktaIdentityProviderList;
import com.stormpath.sdk.application.webconfig.ApplicationWebConfig;
import com.stormpath.sdk.application.webconfig.ApplicationWebConfigStatus;
import com.stormpath.sdk.application.webconfig.ChangePasswordConfig;
import com.stormpath.sdk.application.webconfig.ForgotPasswordConfig;
import com.stormpath.sdk.application.webconfig.LoginConfig;
import com.stormpath.sdk.application.webconfig.MeConfig;
import com.stormpath.sdk.application.webconfig.Oauth2Config;
import com.stormpath.sdk.application.webconfig.RegisterConfig;
import com.stormpath.sdk.application.webconfig.VerifyEmailConfig;
import com.stormpath.sdk.authc.AuthenticationRequest;
import com.stormpath.sdk.authc.AuthenticationResult;
import com.stormpath.sdk.directory.AccountStore;
import com.stormpath.sdk.directory.AccountStoreVisitor;
import com.stormpath.sdk.directory.CustomData;
import com.stormpath.sdk.directory.Directory;
import com.stormpath.sdk.directory.DirectoryCriteria;
import com.stormpath.sdk.group.CreateGroupRequest;
import com.stormpath.sdk.group.Group;
import com.stormpath.sdk.group.GroupCriteria;
import com.stormpath.sdk.group.GroupList;
import com.stormpath.sdk.idsite.IdSiteCallbackHandler;
import com.stormpath.sdk.idsite.IdSiteUrlBuilder;
import com.stormpath.sdk.impl.authc.DefaultOktaAuthNAuthenticator;
import com.stormpath.sdk.impl.authc.DefaultUsernamePasswordRequest;
import com.stormpath.sdk.impl.directory.DefaultDirectory;
import com.stormpath.sdk.impl.directory.OktaDirectory;
import com.stormpath.sdk.impl.ds.InternalDataStore;
import com.stormpath.sdk.impl.oauth.DefaultOAuthTokenRevocator;
import com.stormpath.sdk.impl.okta.OktaApiPaths;
import com.stormpath.sdk.impl.okta.OktaApplicationAccountStoreMapping;
import com.stormpath.sdk.impl.resource.AbstractResource;
import com.stormpath.sdk.impl.resource.Property;
import com.stormpath.sdk.oauth.IdSiteAuthenticator;
import com.stormpath.sdk.oauth.OAuthBearerRequestAuthenticator;
import com.stormpath.sdk.oauth.OAuthClientCredentialsGrantRequestAuthenticator;
import com.stormpath.sdk.oauth.OAuthPasswordGrantRequestAuthenticator;
import com.stormpath.sdk.oauth.OAuthPolicy;
import com.stormpath.sdk.oauth.OAuthRefreshTokenRequestAuthenticator;
import com.stormpath.sdk.oauth.OAuthStormpathFactorChallengeGrantRequestAuthenticator;
import com.stormpath.sdk.oauth.OAuthStormpathSocialGrantRequestAuthenticator;
import com.stormpath.sdk.oauth.OAuthTokenRevocator;
import com.stormpath.sdk.organization.OrganizationCriteria;
import com.stormpath.sdk.provider.OktaProvider;
import com.stormpath.sdk.provider.Provider;
import com.stormpath.sdk.provider.ProviderAccountRequest;
import com.stormpath.sdk.provider.ProviderAccountResult;
import com.stormpath.sdk.resource.ResourceException;
import com.stormpath.sdk.saml.SamlCallbackHandler;
import com.stormpath.sdk.saml.SamlIdpUrlBuilder;
import com.stormpath.sdk.saml.SamlPolicy;
import com.stormpath.sdk.tenant.Tenant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OktaApplication extends AbstractResource implements Application, OAuthApplication {

    private final Directory OKTA_TENANT_DIR;

    private final ApplicationAccountStoreMapping defaultAccountStoreMapping;

    private String name;

    public OktaApplication(String clientId, InternalDataStore dataStore) {
        this(clientId, dataStore, new LinkedHashMap<String, Object>());
    }

    public OktaApplication(String clientId, InternalDataStore dataStore, Map<String, Object> properties) {
        super(dataStore, properties);
        this.OKTA_TENANT_DIR = new OktaDirectory(clientId, dataStore);

        Map<String, Object> mappingProperties = new LinkedHashMap<>();
        mappingProperties.put("href", dataStore.getBaseUrl());
        mappingProperties.put("application", this);
        mappingProperties.put("accountStore", OKTA_TENANT_DIR);

        defaultAccountStoreMapping = new DefaultApplicationAccountStoreMapping(dataStore, mappingProperties) {
            @Override
            public AccountStore getAccountStore() {
                return OKTA_TENANT_DIR;
            }
        };
        defaultAccountStoreMapping.setAccountStore(OKTA_TENANT_DIR);
    }

    @Override
    public AccountLinkingPolicy getAccountLinkingPolicy() {
        throw new UnsupportedOperationException("getAccountLinkingPolicy() method hasn't been implemented.");
    }

    @Override
    public void save() {
        throw new UnsupportedOperationException("save() method hasn't been implemented.");
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException("delete() method hasn't been implemented.");
    }

    @Override
    public Map<String, Property> getPropertyDescriptors() {
        throw new UnsupportedOperationException("getPropertyDescriptors() method hasn't been implemented.");
    }

    @Override
    public String getHref() {
        return getDataStore().getBaseUrl();
    }

    @Override
    public Date getCreatedAt() {
        throw new UnsupportedOperationException("getCreatedAt() method hasn't been implemented.");
    }

    @Override
    public Date getModifiedAt() {
        throw new UnsupportedOperationException("getModifiedAt() method hasn't been implemented.");
    }

    @Override
    public CustomData getCustomData() {
        throw new UnsupportedOperationException("getCustomData() method hasn't been implemented.");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Application setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException("getDescription() method hasn't been implemented.");
    }

    @Override
    public Application setDescription(String description) {
        throw new UnsupportedOperationException("setDescription() method hasn't been implemented.");
    }

    @Override
    public ApplicationStatus getStatus() {
        throw new UnsupportedOperationException("getStatus() method hasn't been implemented.");
    }

    @Override
    public Application setStatus(ApplicationStatus status) {
        throw new UnsupportedOperationException("setStatus() method hasn't been implemented.");
    }

    @Override
    public AccountList getAccounts() {
        throw new UnsupportedOperationException("getAccounts() method hasn't been implemented.");
    }

    @Override
    public AccountList getAccounts(Map<String, Object> queryParams) {
        throw new UnsupportedOperationException("getAccounts() method hasn't been implemented.");
    }

    @Override
    public AccountList getAccounts(AccountCriteria criteria) {
        throw new UnsupportedOperationException("getAccounts() method hasn't been implemented.");
    }

    @Override
    public Account createAccount(Account account) throws ResourceException {
        return OKTA_TENANT_DIR.createAccount(account);
    }

    @Override
    public Account createAccount(CreateAccountRequest request) throws ResourceException {
        return OKTA_TENANT_DIR.createAccount(request);
    }

    @Override
    public GroupList getGroups() {
        throw new UnsupportedOperationException("getGroups() method hasn't been implemented.");
    }

    @Override
    public GroupList getGroups(Map<String, Object> queryParams) {
        throw new UnsupportedOperationException("getGroups() method hasn't been implemented.");
    }

    @Override
    public GroupList getGroups(GroupCriteria criteria) {
        throw new UnsupportedOperationException("getGroups() method hasn't been implemented.");
    }

    @Override
    public Group createGroup(Group group) throws ResourceException {
        throw new UnsupportedOperationException("createGroup() method hasn't been implemented.");
    }

    @Override
    public Group createGroup(CreateGroupRequest request) {
        throw new UnsupportedOperationException("createGroup() method hasn't been implemented.");
    }

    @Override
    public Tenant getTenant() {
        throw new UnsupportedOperationException("getTenant() method hasn't been implemented.");
    }

    @Override
    public PasswordResetToken sendPasswordResetEmail(String email) throws ResourceException {

        OktaForgotPasswordRequest request = getDataStore().instantiate(OktaForgotPasswordRequest.class);
        request.setUsername(email);
        request.setFactorType("EMAIL");
        request.setRelayState("/");

        OktaForgotPasswordResult result = getDataStore().create(OktaApiPaths.PASSWORD_RECOVERY, request, OktaForgotPasswordResult.class);

        return null;

    }

    @Override
    public PasswordResetToken sendPasswordResetEmail(String email, AccountStore accountStore) throws ResourceException {
        throw new UnsupportedOperationException("sendPasswordResetEmail() method hasn't been implemented.");
    }

    @Override
    public Account verifyPasswordResetToken(String token) {
        throw new UnsupportedOperationException("verifyPasswordResetToken() method hasn't been implemented.");
    }

    @Override
    public Account resetPassword(String passwordResetToken, String newPassword) {
        throw new UnsupportedOperationException("resetPassword() method hasn't been implemented.");
    }

    @Override
    public AuthenticationResult authenticateAccount(AuthenticationRequest request) throws ResourceException {

        return new DefaultOktaAuthNAuthenticator(getDataStore()).authenticate((DefaultUsernamePasswordRequest)request);
    }

    @Override
    public ProviderAccountResult getAccount(ProviderAccountRequest request) {

        return new DefaultOktaAuthNAuthenticator(getDataStore()).getAccount(request);
    }

    @Override
    public ApplicationAccountStoreMappingList getAccountStoreMappings() {

        OktaIdentityProviderList oktaIdentityProviderList = getDataStore().getResource(OktaApiPaths.API_V1 + "idps", OktaIdentityProviderList.class);

        Collection<ApplicationAccountStoreMapping> mappingList = new ArrayList<>();
        mappingList.add(defaultAccountStoreMapping);

        // TODO: fix nested classes
        for(final OktaProvider provider : oktaIdentityProviderList.getIdentityProviders() ) {
            ApplicationAccountStoreMapping mapping = new OktaApplicationAccountStoreMapping(this.getDataStore());
            mapping.setApplication(this);
            mapping.setAccountStore(new AccountStore() {
                @Override
                public void accept(AccountStoreVisitor visitor) {
                    visitor.visit(new DefaultDirectory(getDataStore()) {
                        @Override
                        public Provider getProvider() {
                            return provider;
                        }
                    });
                }

                @Override
                public String getHref() {
                    return null;
                }
            });

            mappingList.add(mapping);
        }

        return new ApplicationAccountStoreMappingCollectionBackedList(mappingList);
    }

    @Override
    public ApplicationAccountStoreMappingList getAccountStoreMappings(Map<String, Object> queryParams) {
        return getAccountStoreMappings();
    }

    @Override
    public ApplicationAccountStoreMappingList getAccountStoreMappings(ApplicationAccountStoreMappingCriteria criteria) {
        return getAccountStoreMappings();
    }


    @Override
    public ApplicationAccountStoreMapping createAccountStoreMapping(ApplicationAccountStoreMapping mapping) throws ResourceException {
        throw new UnsupportedOperationException("createAccountStoreMapping() method hasn't been implemented.");
    }

    @Override
    public ApplicationAccountStoreMapping addAccountStore(AccountStore accountStore) throws ResourceException {
        throw new UnsupportedOperationException("addAccountStore() method hasn't been implemented.");
    }

    @Override
    public ApiKey getApiKey(String id) throws ResourceException, IllegalArgumentException {
        throw new UnsupportedOperationException("getApiKey() method hasn't been implemented.");
    }

    @Override
    public ApiKey getApiKey(String id, ApiKeyOptions options) throws ResourceException, IllegalArgumentException {
        throw new UnsupportedOperationException("getApiKey() method hasn't been implemented.");
    }

    @Override
    public IdSiteUrlBuilder newIdSiteUrlBuilder() {
        throw new UnsupportedOperationException("newIdSiteUrlBuilder() method hasn't been implemented.");
    }

    @Override
    public SamlIdpUrlBuilder newSamlIdpUrlBuilder() {
        throw new UnsupportedOperationException("newSamlIdpUrlBuilder() method hasn't been implemented.");
    }

    @Override
    public IdSiteCallbackHandler newIdSiteCallbackHandler(Object httpRequest) {
        throw new UnsupportedOperationException("newIdSiteCallbackHandler() method hasn't been implemented.");
    }

    @Override
    public SamlCallbackHandler newSamlCallbackHandler(Object httpRequest) {
        throw new UnsupportedOperationException("newSamlCallbackHandler() method hasn't been implemented.");
    }

    @Override
    public void sendVerificationEmail(VerificationEmailRequest verificationEmailRequest) {
        throw new UnsupportedOperationException("sendVerificationEmail() method hasn't been implemented.");
    }

    @Override
    public ApplicationAccountStoreMapping addAccountStore(String hrefOrName) {
        throw new UnsupportedOperationException("addAccountStore() method hasn't been implemented.");
    }

    @Override
    public ApplicationAccountStoreMapping addAccountStore(DirectoryCriteria criteria) {
        throw new UnsupportedOperationException("addAccountStore() method hasn't been implemented.");
    }

    @Override
    public ApplicationAccountStoreMapping addAccountStore(OrganizationCriteria criteria) {
        throw new UnsupportedOperationException("addAccountStore() method hasn't been implemented.");
    }

    @Override
    public ApplicationAccountStoreMapping addAccountStore(GroupCriteria criteria) {
        throw new UnsupportedOperationException("addAccountStore() method hasn't been implemented.");
    }

    @Override
    public Application saveWithResponseOptions(ApplicationOptions responseOptions) {
        throw new UnsupportedOperationException("saveWithResponseOptions() method hasn't been implemented.");
    }

    @Override
    public OAuthPolicy getOAuthPolicy() {
        throw new UnsupportedOperationException("getOAuthPolicy() method hasn't been implemented.");
    }

    @Override
    public SamlPolicy getSamlPolicy() {
        throw new UnsupportedOperationException("getSamlPolicy() method hasn't been implemented.");
    }

    @Override
    public ApplicationWebConfig getWebConfig() {

        return new ApplicationWebConfig() {
            @Override
            public void save() {
                throw new UnsupportedOperationException("save() method hasn't been implemented.");
            }

            @Override
            public String getHref() {
                throw new UnsupportedOperationException("getHref() method hasn't been implemented.");
            }

            @Override
            public Date getCreatedAt() {
                throw new UnsupportedOperationException("getCreatedAt() method hasn't been implemented.");
            }

            @Override
            public Date getModifiedAt() {
                throw new UnsupportedOperationException("getModifiedAt() method hasn't been implemented.");
            }

            @Override
            public String getDomainName() {
                return "OktaDirectory-DomainName-Not-Used";
            }

            @Override
            public String getDnsLabel() {
                throw new UnsupportedOperationException("getDnsLabel() method hasn't been implemented.");
            }

            @Override
            public ApplicationWebConfig setDnsLabel(String dnsLabel) {
                throw new UnsupportedOperationException("setDnsLabel() method hasn't been implemented.");
            }

            @Override
            public ApplicationWebConfigStatus getStatus() {
                return ApplicationWebConfigStatus.ENABLED;
            }

            @Override
            public ApplicationWebConfig setStatus(ApplicationWebConfigStatus status) {
                throw new UnsupportedOperationException("setStatus() method hasn't been implemented.");
            }

            @Override
            public ApiKey getSigningApiKey() {
                throw new UnsupportedOperationException("getSigningApiKey() method hasn't been implemented.");
            }

            @Override
            public ApplicationWebConfig setSigningApiKey(ApiKey apiKey) {
                throw new UnsupportedOperationException("setSigningApiKey() method hasn't been implemented.");
            }

            @Override
            public Oauth2Config getOAuth2() {
                throw new UnsupportedOperationException("getOAuth2() method hasn't been implemented.");
            }

            @Override
            public RegisterConfig getRegister() {
                throw new UnsupportedOperationException("getRegister() method hasn't been implemented.");
            }

            @Override
            public LoginConfig getLogin() {
                return new LoginConfig() {
                    private boolean enabled = true;
                    @Override
                    public LoginConfig setEnabled(Boolean enabled) {
                         this.enabled = enabled;
                         return this;
                    }

                    @Override
                    public Boolean isEnabled() {
                        return enabled;
                    }
                };
            }

            @Override
            public VerifyEmailConfig getVerifyEmail() {
                throw new UnsupportedOperationException("getVerifyEmail() method hasn't been implemented.");
            }

            @Override
            public ForgotPasswordConfig getForgotPassword() {
                throw new UnsupportedOperationException("getForgotPassword() method hasn't been implemented.");
            }

            @Override
            public ChangePasswordConfig getChangePassword() {
                throw new UnsupportedOperationException("getChangePassword() method hasn't been implemented.");
            }

            @Override
            public MeConfig getMe() {
                throw new UnsupportedOperationException("getMe() method hasn't been implemented.");
            }

            @Override
            public Application getApplication() {
                throw new UnsupportedOperationException("getApplication() method hasn't been implemented.");
            }

            @Override
            public Tenant getTenant() {
                throw new UnsupportedOperationException("getTenant() method hasn't been implemented.");
            }
        };
    }

    @Override
    public List<String> getAuthorizedCallbackUris() {
        throw new UnsupportedOperationException("getAuthorizedCallbackUris() method hasn't been implemented.");
    }

    @Override
    public Application setAuthorizedCallbackUris(List<String> authorizedCallbackUris) {
        throw new UnsupportedOperationException("setAuthorizedCallbackUris() method hasn't been implemented.");
    }

    @Override
    public Application addAuthorizedCallbackUri(String authorizedCallbackUri) {
        throw new UnsupportedOperationException("addAuthorizedCallbackUri() method hasn't been implemented.");
    }

    @Override
    public List<String> getAuthorizedOriginUris() {
        throw new UnsupportedOperationException("getAuthorizedOriginUris() method hasn't been implemented.");
    }

    @Override
    public Application setAuthorizedOriginUris(List<String> authorizedOriginUris) {
        throw new UnsupportedOperationException("setAuthorizedOriginUris() method hasn't been implemented.");
    }

    @Override
    public Application addAuthorizedOriginUri(String authorizedOriginUri) {
        throw new UnsupportedOperationException("addAuthorizedOriginUri() method hasn't been implemented.");
    }

    @Override
    public AccountStore getDefaultAccountStore() {
        return OKTA_TENANT_DIR;
    }

    @Override
    public void setDefaultAccountStore(AccountStore accountStore) {
        throw new UnsupportedOperationException("setDefaultAccountStore() method hasn't been implemented.");
    }

    @Override
    public AccountStore getDefaultGroupStore() {
        return OKTA_TENANT_DIR;
    }

    @Override
    public void setDefaultGroupStore(AccountStore accountStore) {
        throw new UnsupportedOperationException("setDefaultGroupStore() method hasn't been implemented.");
    }


    @Override
    public OAuthClientCredentialsGrantRequestAuthenticator createClientCredentialsGrantAuthenticator() {
        throw new UnsupportedOperationException("createClientCredentialsGrantAuthenticator() method hasn't been implemented.");
    }

    @Override
    public OAuthStormpathSocialGrantRequestAuthenticator createStormpathSocialGrantAuthenticator() {
        throw new UnsupportedOperationException("createStormpathSocialGrantAuthenticator() method hasn't been implemented.");
    }

    @Override
    public OAuthStormpathFactorChallengeGrantRequestAuthenticator createStormpathFactorChallengeGrantAuthenticator() {
        throw new UnsupportedOperationException("createStormpathFactorChallengeGrantAuthenticator() method hasn't been implemented.");
    }

    @Override
    public OAuthPasswordGrantRequestAuthenticator createPasswordGrantAuthenticator() {
        // TODO clean this up, maybe these Authenticators should be _merged_ in with the OktaAuthN?
        return new OktaOAuthPasswordGrantRequestAuthenticator(this, getDataStore(), "/oauth2/v1/token", new DefaultOktaAuthNAuthenticator(getDataStore()), createOAuhtTokenRevocator(), createRefreshGrantAuthenticator());
    }

    @Override
    public OAuthRefreshTokenRequestAuthenticator createRefreshGrantAuthenticator() {
        return new OktaOAuthRefreshTokenRequestAuthenticator(this, getDataStore(), "/oauth2/v1/token", new DefaultOktaAuthNAuthenticator(getDataStore()), createOAuhtTokenRevocator());
    }

    @Override
    public OAuthBearerRequestAuthenticator createJwtAuthenticator() {
        return new OktaOAuthRequestAuthenticator(this, getDataStore(), new DefaultOktaAuthNAuthenticator(getDataStore()));
    }

    public IdSiteAuthenticator createIdSiteAuthenticator() {
        throw new UnsupportedOperationException("createIdSiteAuthenticator() method hasn't been implemented.");
    }

    @Override
    public OAuthTokenRevocator createOAuhtTokenRevocator() {
        return new DefaultOAuthTokenRevocator(this, getDataStore(), "/oauth2/v1/revoke");
    }


    public static class ApplicationAccountStoreMappingCollectionBackedList implements ApplicationAccountStoreMappingList {

        private final Collection<ApplicationAccountStoreMapping> accountStoreMappings;

        public ApplicationAccountStoreMappingCollectionBackedList(Collection<ApplicationAccountStoreMapping> accountStoreMappings) {
            this.accountStoreMappings = accountStoreMappings;
        }

        @Override
        public String getHref() {
            return null;
        }

        @Override
        public int getOffset() {
            return 0;
        }

        @Override
        public int getLimit() {
            return getSize();
        }

        @Override
        public int getSize() {
            return accountStoreMappings.size();
        }

        @Override
        public ApplicationAccountStoreMapping single() {
            Iterator<ApplicationAccountStoreMapping> iterator = iterator();
            if (!iterator.hasNext()) {
                throw new IllegalStateException("This list is empty while it was expected to contain one (and only one) element.");
            }
            ApplicationAccountStoreMapping itemToReturn = iterator.next();
            if (iterator.hasNext()) {
                throw new IllegalStateException("Only a single resource was expected, but this list contains more than one item.");
            }
            return itemToReturn;
        }

        @Override
        public Iterator<ApplicationAccountStoreMapping> iterator() {
            return accountStoreMappings.iterator();
        }
    }
}