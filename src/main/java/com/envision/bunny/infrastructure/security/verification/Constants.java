package com.envision.bunny.infrastructure.security.verification;

/**
 * @author jingjing.dong
 * @since 2021/4/8-14:45
 */
class Constants {
    static final String INPUT_CODE_NAME = "code";
    static final String VERIFY_SESSION_NAME = "verify_code";
    static final String LDAP_ACCOUNT = "****";
    static final String LDAP_PASSWORD = "*****";
    static final String LDAP_URL = "ldap://envisioncn.com:389/";
    static final String LDAP_BASE_DOMAIN = "DC=envisioncn,DC=com";
    static final String LDAP_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";
}
