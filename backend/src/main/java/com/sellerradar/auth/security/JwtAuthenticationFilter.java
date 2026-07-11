package com.sellerradar.auth.security;

import com.sellerradar.auth.jwt.JwtClaims;
import com.sellerradar.auth.jwt.JwtTokenProvider;
import com.sellerradar.common.error.BusinessException;
import com.sellerradar.user.domain.User;
import com.sellerradar.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;
	private final UserRepository userRepository;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
		this.jwtTokenProvider = jwtTokenProvider;
		this.userRepository = userRepository;
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain
	) throws ServletException, IOException {
		String accessToken = resolveBearerToken(request);
		if (accessToken != null) {
			authenticate(accessToken);
		}
		filterChain.doFilter(request, response);
	}

	private void authenticate(String accessToken) {
		try {
			JwtClaims claims = jwtTokenProvider.parseAccessToken(accessToken);
			User user = userRepository.findActiveById(claims.userId()).orElse(null);
			if (user == null) {
				SecurityContextHolder.clearContext();
				return;
			}
			AuthenticatedUser principal = new AuthenticatedUser(
					user.getId(),
					user.getEmail(),
					user.getRole(),
					user.getPlanCode()
			);
			List<SimpleGrantedAuthority> authorities = List.of(
					new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
			);
			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(principal, null, authorities);
			SecurityContextHolder.getContext().setAuthentication(authentication);
		} catch (BusinessException exception) {
			SecurityContextHolder.clearContext();
		}
	}

	private String resolveBearerToken(HttpServletRequest request) {
		String authorization = request.getHeader(AUTHORIZATION_HEADER);
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			return null;
		}
		String token = authorization.substring(BEARER_PREFIX.length());
		return token.isBlank() ? null : token;
	}
}
