package com.jsb.recebimentos.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class MfaEnforcementFilter extends OncePerRequestFilter {
    public static final String MFA_VERIFIED = "ESC_MFA_VERIFIED";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean loggedIn = authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
        HttpSession session = request.getSession(false);
        boolean mfaVerified = session != null && Boolean.TRUE.equals(session.getAttribute(MFA_VERIFIED));
        boolean allowedWithoutMfa = path.equals("/mfa") || path.equals("/login") || path.equals("/setup")
                || path.equals("/error") || path.startsWith("/images/") || path.startsWith("/favicon");
        if (loggedIn && !mfaVerified && !allowedWithoutMfa) {
            response.sendRedirect(request.getContextPath() + "/mfa");
            return;
        }
        chain.doFilter(request, response);
    }
}
