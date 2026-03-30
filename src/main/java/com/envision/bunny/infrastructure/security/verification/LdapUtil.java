package com.envision.bunny.infrastructure.security.verification;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.Control;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

/**
 * Ldap验证，核心逻辑是先获取CN，然后使用CN和传入的密码尝试链接，如果可以登陆则验证成功，否则验证失败
 *
 * @author jingjing.dong
 * @since 2021/4/21-15:31
 */
@Slf4j(topic = "LDAP Authentication")
public class LdapUtil {
    private static final Hashtable<String, String> env = new Hashtable<>();
    private static final Control[] Controls = null;
    static {
        env.put("java.naming.factory.initial", Constants.LDAP_FACTORY);
        env.put("java.naming.provider.url", Constants.LDAP_URL + Constants.LDAP_BASE_DOMAIN);
        env.put("java.naming.security.authentication", "simple");
        env.put("java.naming.security.principal", Constants.LDAP_ACCOUNT);
        env.put("java.naming.security.credentials", Constants.LDAP_PASSWORD);
    }

    public static boolean authenticate(String userName, String password) {
        LdapContext ctx = initConnect();
        try {
            NamingEnumeration<SearchResult> results = searchByName(ctx, userName);
            if (!results.hasMoreElements()) {
                log.debug("there is no user named [{}] found in LDAP", userName);
                return false;
            }
            return verifyPassword(password, ctx, results.nextElement());
        } catch (NamingException var6) {
            return false;
        } finally {
            closeContext(ctx);
        }
    }

    private static LdapContext initConnect() {
        try {
            return new InitialLdapContext(env, Controls);
        } catch (NamingException e) {
            throw new BizException(ErrorCode.REMOTE_ERROR);
        }
    }

    private static NamingEnumeration<SearchResult> searchByName(LdapContext ctx, String userName) throws NamingException {
        SearchControls constraints = new SearchControls();
        constraints.setSearchScope(2);
        return ctx.search("", "sAMAccountName=" + userName, constraints);
    }

    private static boolean verifyPassword(String password, LdapContext ctx, SearchResult result) {
        try {
            String userCN = result.getName() + "," + Constants.LDAP_BASE_DOMAIN;
            ctx.addToEnvironment("java.naming.security.principal", userCN);
            ctx.addToEnvironment("java.naming.security.credentials", password);
            ctx.reconnect(Controls);
        } catch (NamingException e) {
            log.debug("Credentials of user [{}] is incorrect", result.getName());
            return false;
        }
        return true;
    }

    private static void closeContext(DirContext context) {
        if (context != null) {
            try {
                context.close();
            } catch (NamingException var2) {
                log.debug("Could not close JNDI DirContext", var2);
            } catch (Throwable var3) {
                log.debug("Unexpected exception on closing JNDI DirContext", var3);
            }
        }

    }
}
