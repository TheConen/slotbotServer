package de.webalf.slotbot.configuration.authentication.api;

import de.webalf.slotbot.exception.ForbiddenException;
import de.webalf.slotbot.model.authentication.ApiToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import static de.webalf.slotbot.constant.AuthorizationCheckValues.ROLE_PREFIX;

/**
 * @author Alf
 * @since 23.09.2020
 */
@Component
public class TokenAuthFilter extends OncePerRequestFilter {
	@Value("${slotbot.auth.token.name:slotbot-auth-token}")
	private String tokenName;

	private final TokenAuthProvider tokenAuthProvider;
	private final HandlerExceptionResolver resolver;

	@Autowired
	public TokenAuthFilter(TokenAuthProvider tokenAuthProvider, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
		this.tokenAuthProvider = tokenAuthProvider;
		this.resolver = resolver;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws IOException, ServletException {
		// check if header contains auth token
		final String authToken = request.getHeader(tokenName);

		// if there is an auth token, create an Authentication object
		if (authToken != null) {
			Set<GrantedAuthority> grantedAuthorities;
			try {
				grantedAuthorities = mapAuthorities(authToken);
			} catch (ForbiddenException ex) {
				resolver.resolveException(request, response, null, ex);
				return;
			}
			final Authentication auth = new SlotbotAuthentication(authToken, grantedAuthorities);
			SecurityContextHolder.getContext().setAuthentication(auth);
		}

		// forward the request
		filterChain.doFilter(request, response);
	}

	private Set<GrantedAuthority> mapAuthorities(@NotBlank String token) throws ForbiddenException {
		final ApiToken apiToken = tokenAuthProvider.getApiToken(token);
		return Collections.singleton(new SimpleGrantedAuthority(ROLE_PREFIX + apiToken.getType().name()));
	}
}