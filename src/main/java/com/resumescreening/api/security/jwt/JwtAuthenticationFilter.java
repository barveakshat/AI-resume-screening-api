package com.resumescreening.api.security.jwt;

import com.resumescreening.api.security.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Extract JWT from Authorization header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        // Check if header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token (remove "Bearer " prefix)
        jwt = authHeader.substring(7);

        try {
            // Extract username from token
            userEmail = jwtUtil.extractUsername(jwt);

            // If username exists and user not already authenticated
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Validate token
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    log.debug("JWT valid for user: {}", userEmail);
                    // Create authentication object
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

                    // Set additional details
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        }
        catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("JWT expired: {}", e.getMessage());
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.debug("JWT malformed: {}", e.getMessage());
        } catch (io.jsonwebtoken.SignatureException e) {
            log.debug("JWT signature invalid: {}", e.getMessage());
        } catch (Exception e) {
            log.debug("JWT processing error: {}", e.getMessage());
        }
//        catch (Exception e) {
//            // Token invalid - continue without authentication
//            // Security will reject if endpoint requires auth
//        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }
}
