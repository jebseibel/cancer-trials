package com.seibel.cancer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed.origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Serves the React app's own routes from the bundled SPA.
     *
     * <p>A client-side route is not a file on disk: the server has to return {@code index.html}
     * and let the router resolve the path in the browser. Without this, opening or refreshing
     * {@code /login} or {@code /ranked-trials} directly — or following a link to one — returns
     * 404 rather than the app.
     *
     * <p>Only matters for the bundled deployment jar, where Spring serves the frontend; the Vite
     * dev server does its own fallback. It went unnoticed until endpoint security was switched
     * on, because {@code permitAll()} plus the dev server hid both halves of the problem.
     *
     * <p>Listed explicitly rather than a catch-all, so a genuinely wrong URL still 404s instead
     * of silently rendering the app.
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        for (String route : new String[]{
                "/login", "/ranked-trials", "/saved-trials", "/diagnosis",
                "/variants", "/prior-treatment", "/ingestion", "/trials"}) {
            registry.addViewController(route).setViewName("forward:/index.html");
        }
        // Trial detail carries an extid: /trials/{extid}
        registry.addViewController("/trials/*").setViewName("forward:/index.html");
    }
}

