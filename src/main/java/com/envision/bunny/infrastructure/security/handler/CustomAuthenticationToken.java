package com.envision.bunny.infrastructure.security.handler;

import com.envision.bunny.module.user.domain.User;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * @author jingjing.dong
 * @since 2021/3/31-18:37
 */
@Getter
@Setter
public class CustomAuthenticationToken extends AbstractAuthenticationToken {
    private String code;
    private String successUrl;
    private User user;

    public CustomAuthenticationToken(String code, String successUrl) {
        super(null);
        this.code = code;
        this.successUrl = successUrl;
        this.setAuthenticated(false);
    }

    public CustomAuthenticationToken(User user, String successUrl, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.user = user;
        this.successUrl = successUrl;
        super.setAuthenticated(true);
    }
    @Override
    public Object getCredentials() {
        return null;
    }
    @Override
    public Object getPrincipal() {
        return this.user;
    }
    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException(
                    "Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }

        super.setAuthenticated(false);
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
    }


}
