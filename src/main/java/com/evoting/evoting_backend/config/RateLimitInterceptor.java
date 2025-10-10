package com.evoting.evoting_backend.config;

import com.evoting.evoting_backend.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String endpoint = request.getRequestURI();
        String clientIp = getClientIpAddress(request);
        String method = request.getMethod();
        
        // Skip rate limiting for actuator endpoints
        if (endpoint.startsWith("/actuator")) {
            return true;
        }
        
        // Apply IP-based rate limiting
        if (!rateLimitService.isIpAllowed(clientIp, endpoint)) {
            response.setStatus(429); // Too Many Requests
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Rate limit exceeded\", \"code\": \"RATE_LIMIT_EXCEEDED\", \"message\": \"Too many requests from your IP address\"}");
            return false;
        }
        
        // Apply user-based rate limiting for authenticated requests
        if (isAuthenticated()) {
            String username = getCurrentUsername();
            if (!rateLimitService.isUserAllowed(username, endpoint)) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Rate limit exceeded\", \"code\": \"RATE_LIMIT_EXCEEDED\", \"message\": \"Too many requests from your account\"}");
                return false;
            }
        }
        
        // Add rate limit headers for information
        response.setHeader("X-RateLimit-Limit", "100");
        response.setHeader("X-RateLimit-Remaining", "99");
        
        return true;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean isAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null &&
               SecurityContextHolder.getContext().getAuthentication().isAuthenticated() &&
               !"anonymousUser".equals(SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }
    
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }
}