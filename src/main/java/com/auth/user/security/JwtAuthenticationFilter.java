package com.auth.user.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@AllArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private JwtUtils jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String jwt = jwtTokenProvider.getJwtFromHeader(request);
            if (jwt != null && jwtTokenProvider.validateToken(jwt)) {
                String phoneNumber = jwtTokenProvider.getPhoneNumberFromJwt(jwt);
                UserDetails userDetails = userDetailsService.loadUserByUsername(phoneNumber);
                if (userDetails != null) {
                    setSecurityContextHolder(request, userDetails);
                }
            }
        } catch (Exception e) {
            logger.error(JwtUtils.INVALID_JWT_TOKEN_MSG, e);
        }

        // required to continue the filter chain
        filterChain.doFilter(request, response);
    }

    private static void setSecurityContextHolder(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
