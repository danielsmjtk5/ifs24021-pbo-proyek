package org.delcom.app.configs;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class StartupInfoLogger implements ApplicationListener<ApplicationReadyEvent> {

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Environment env = event.getApplicationContext().getEnvironment();

        // Default port = 8080
        String port = env.getProperty("server.port", "8080");

        // Default Spring Boot context-path = "" (NOT "/")
        String contextPath = env.getProperty("server.servlet.context-path", "");

        // ✔ FIX: Replace null/trim checks with StringUtils.hasText() to avoid IDE warnings
        if (!StringUtils.hasText(contextPath) || "/".equals(contextPath)) {
            contextPath = "";
        }

        // Detect LiveReload
        boolean liveReloadEnabled =
                env.getProperty("spring.devtools.livereload.enabled", Boolean.class, false);
        String liveReloadPort =
                env.getProperty("spring.devtools.livereload.port", "35729");

        // Host default = localhost
        String host = env.getProperty("server.address", "localhost");

        // ANSI Colors
        String GREEN = "\u001B[32m";
        String CYAN = "\u001B[36m";
        String YELLOW = "\u001B[33m";
        String RESET = "\u001B[0m";

        System.out.println();
        System.out.println(GREEN + "Application started successfully!" + RESET);
        System.out.println(CYAN + "> URL: http://" + host + ":" + port + contextPath + RESET);
        System.out.println(
                liveReloadEnabled
                        ? (YELLOW + "> LiveReload: ENABLED (port " + liveReloadPort + ")" + RESET)
                        : (YELLOW + "> LiveReload: DISABLED" + RESET)
        );
        System.out.println();
    }
}
