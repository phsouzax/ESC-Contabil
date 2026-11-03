package com.jsb.recebimentos.security;

import com.jsb.recebimentos.entity.AppUser;
import com.jsb.recebimentos.repository.AppUserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AppUserRepository users;

    public LoginSuccessHandler(AppUserRepository users) { this.users = users; }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        AppUser user = users.findByUsername(authentication.getName())
                .orElseThrow(() -> new ServletException("Conta autenticada não encontrada."));
        LocalDateTime lastMfa = user.getLastMfaAt();
        boolean needsMfa = lastMfa == null || !lastMfa.isAfter(LocalDateTime.now().minusDays(15));
        request.getSession(true).setAttribute(MfaEnforcementFilter.MFA_VERIFIED, !needsMfa);
        response.sendRedirect(needsMfa ? "/mfa" : "/");
    }
}
