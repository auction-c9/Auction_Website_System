package com.example.auction_management.config;

import com.example.auction_management.service.impl.CustomUserDetailsService;
import com.example.auction_management.util.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, CustomUserDetailsService customUserDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.customUserDetailsService = customUserDetailsService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        return
                // Các endpoint auth không cần xác thực
                path.startsWith("/api/auth/login") ||
                        path.equals("/api/auth/register") ||
                        path.equals("/api/auth/google") ||
                        path.equals("/api/auth/forgot-password") ||

                        // Các endpoint GET public trong /api/auctions
                        (method.equals("GET") && (
                                path.equals("/api/auctions") ||
                                        path.startsWith("/api/auctions/status/") ||
                                        path.startsWith("/api/auctions/ongoing") ||
                                        path.startsWith("/api/auctions/product/") ||
                                        path.matches("/api/auctions/\\d+")
                        )) ||

                        // Các endpoint GET public khác
                        (method.equals("GET") && path.startsWith("/api/categories/")) ||
                        (method.equals("GET") && path.startsWith("/api/products/"));
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

            try {
                // Lấy token từ header
                String token = getJwtFromRequest(request);
                if (token != null && jwtTokenProvider.validateToken(token)) {
                    // Lấy thông tin từ token
                    String username = jwtTokenProvider.getUsernameFromToken(token);
                    Integer customerId = jwtTokenProvider.getCustomerIdFromToken(token);
                    String role = jwtTokenProvider.getRoleFromToken(token);
                    logger.info("Role from token: " + role);

                    // Tạo UserDetails và Authentication
                    UserDetails userDetails = customUserDetailsService.loadUserByUsername(username);
                    logger.info("User authorities: " + userDetails.getAuthorities());
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    // Thêm customerId vào details
                    Map<String, Object> details = new HashMap<>();
                    details.put("customerId", customerId);
                    authentication.setDetails(details);

                    // Thiết lập SecurityContext
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                logger.error("Cannot set authentication", e);
            }
            filterChain.doFilter(request, response);
        }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
