package com.jsb.recebimentos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;

/**
 * Abre o navegador padrão quando o sistema termina de iniciar.
 * Só age no perfil de produção local (instalador) e se app.open-browser=true.
 */
@Component
@Profile("mysql-local")
public class BrowserLauncher {
    private final boolean enabled;
    private final int port;

    public BrowserLauncher(@Value("${app.open-browser:false}") boolean enabled,
                           @Value("${server.port:8080}") int port) {
        this.enabled = enabled;
        this.port = port;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void open() {
        if (!enabled) return;
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("http://localhost:" + port));
            }
        } catch (Exception ignored) {
            // Se não abrir sozinho, o usuário acessa http://localhost:8080 manualmente.
        }
    }
}
