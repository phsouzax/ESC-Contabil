package com.jsb.recebimentos.controller;

import com.jsb.recebimentos.entity.AppUser;
import com.jsb.recebimentos.repository.AppUserRepository;
import com.jsb.recebimentos.security.MfaEnforcementFilter;
import com.jsb.recebimentos.security.RecoveryCodeService;
import com.jsb.recebimentos.security.TotpService;
import com.jsb.recebimentos.security.TotpSecretCipher;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
public class AuthController {
    private static final String PENDING_TOTP = "ESC_PENDING_TOTP_SECRET";

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TotpService totp;
    private final RecoveryCodeService recoveryCodes;
    private final TotpSecretCipher secretCipher;

    public AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, TotpService totp,
                          RecoveryCodeService recoveryCodes, TotpSecretCipher secretCipher) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.totp = totp;
        this.recoveryCodes = recoveryCodes;
        this.secretCipher = secretCipher;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String error,
                        @RequestParam(required = false) String logout,
                        Model model) {
        if (users.count() == 0) return "redirect:/setup";
        model.addAttribute("loginError", error != null);
        model.addAttribute("loggedOut", logout != null);
        return "login";
    }

    @GetMapping("/setup")
    public String setup(HttpSession session, Model model) {
        if (users.count() > 0) return "redirect:/login";
        String secret = (String) session.getAttribute(PENDING_TOTP);
        if (secret == null) {
            secret = totp.newSecret();
            session.setAttribute(PENDING_TOTP, secret);
        }
        model.addAttribute("secret", secret);
        model.addAttribute("qrDataUri", totp.qrDataUri(secret, "admin"));
        return "setup-account";
    }

    @PostMapping("/setup")
    public String finishSetup(@RequestParam String username,
                              @RequestParam String password,
                              @RequestParam String confirmPassword,
                              @RequestParam String code,
                              HttpSession session,
                              RedirectAttributes flash) {
        if (users.count() > 0) return "redirect:/login";
        String cleanUsername = username == null ? "" : username.trim();
        String secret = (String) session.getAttribute(PENDING_TOTP);
        if (cleanUsername.isBlank() || cleanUsername.length() > 80) {
            flash.addFlashAttribute("setupError", "Informe um usuário com até 80 caracteres.");
            return "redirect:/setup";
        }
        if (password == null || password.length() < 12) {
            flash.addFlashAttribute("setupError", "A senha precisa ter pelo menos 12 caracteres.");
            return "redirect:/setup";
        }
        if (!password.equals(confirmPassword)) {
            flash.addFlashAttribute("setupError", "As senhas não conferem.");
            return "redirect:/setup";
        }
        if (secret == null || !totp.verify(secret, code)) {
            flash.addFlashAttribute("setupError", "Código inválido. Confira o horário do celular e tente novamente.");
            return "redirect:/setup";
        }
        var oneTimeCodes = recoveryCodes.generate();
        users.save(new AppUser(cleanUsername, passwordEncoder.encode(password), secretCipher.encrypt(secret),
                recoveryCodes.hashAll(oneTimeCodes), LocalDateTime.now()));
        session.removeAttribute(PENDING_TOTP);
        flash.addFlashAttribute("setupComplete", "Conta protegida criada. Entre com seu usuário e senha.");
        flash.addFlashAttribute("recoveryCodes", oneTimeCodes);
        return "redirect:/login";
    }

    @GetMapping("/mfa")
    public String mfa(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        return "mfa";
    }

    @PostMapping("/mfa")
    public String verifyMfa(@RequestParam(required = false) String code,
                            @RequestParam(required = false) String recoveryCode,
                            Principal principal, HttpSession session,
                            RedirectAttributes flash) {
        if (principal == null) return "redirect:/login";
        AppUser user = users.findByUsername(principal.getName()).orElseThrow();
        boolean validTotp = totp.verify(secretCipher.decrypt(user.getTotpSecret()), code);
        boolean validRecovery = !validTotp && recoveryCodes.consume(user, recoveryCode);
        if (!validTotp && !validRecovery) {
            flash.addFlashAttribute("mfaError", "Código inválido ou expirado. Tente o código atual do aplicativo Authenticator.");
            return "redirect:/mfa";
        }
        user.markMfaVerified(LocalDateTime.now());
        users.save(user);
        session.setAttribute(MfaEnforcementFilter.MFA_VERIFIED, true);
        return "redirect:/";
    }
}
