package com.heimdall.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class TokenAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final String frontendUrl;

    public TokenAuthSuccessHandler(JwtService jwtService,
                                   @Value("${heimdall.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.jwtService = jwtService;
        this.frontendUrl = frontendUrl;
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TokenAuthSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        logger.info("Authentication success for OAuth2 user: " + email);
        
        String token = jwtService.generateToken(email);
        logger.debug("Generated JWT for " + email + ": " + token);
        
        String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        String targetUrl = baseUrl + "/?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
