package org.game.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

public final class PlayerAuthenticationToken extends AbstractAuthenticationToken {

	private final UUID token;

	public PlayerAuthenticationToken(UUID token, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.token = token;
		setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return null;
	}

	@Override
	public Object getPrincipal() {
		return token;
	}
}
