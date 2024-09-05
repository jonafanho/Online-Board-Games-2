package org.game.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.game.Main;
import org.game.core.Utilities;
import org.game.repository.PlayerRepository;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

public final class AuthenticationFilter extends GenericFilterBean {

	private final PlayerRepository playerRepository;

	public static final String UUID_HEADER = "uuid";
	public static final String TOKEN_HEADER = "token";

	public AuthenticationFilter(PlayerRepository playerRepository) {
		this.playerRepository = playerRepository;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;

		if (httpServletRequest.getRequestURI().contains("/api/public/") || !httpServletRequest.getRequestURI().contains("/api/")) {
			SecurityContextHolder.getContext().setAuthentication(new PlayerAuthenticationToken(new UUID(0, 0), AuthorityUtils.NO_AUTHORITIES));
		} else {
			Utilities.parseUuid(httpServletRequest.getHeader(UUID_HEADER), httpServletRequest.getHeader(TOKEN_HEADER), (uuid, token) -> {
				if (playerRepository.getPlayerByUuidAndToken(uuid, token).isPresent()) {
					SecurityContextHolder.getContext().setAuthentication(new PlayerAuthenticationToken(token, AuthorityUtils.NO_AUTHORITIES));
				} else {
					writeError(servletResponse, "Invalid credentials! Please register the player again.");
				}
				return null;
			}, () -> {
				writeError(servletResponse, "Invalid UUID or token format!");
				return null;
			});
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private static void writeError(ServletResponse servletResponse, String message) {
		final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
		httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);

		try (final PrintWriter printWriter = httpServletResponse.getWriter()) {
			printWriter.print(message);
			printWriter.flush();
		} catch (IOException e) {
			Main.LOGGER.error("", e);
		}
	}
}
