package com.heimdall.backend.security;

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

    public TokenAuthSuccessHandler(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TokenAuthSuccessHandler.class);

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        logger.info("Authentication success for OAuth2 user: " + email);
        
        String token = jwtService.generateToken(email);
        logger.debug("Generated JWT for " + email + ": " + token);
        
        String targetUrl = "http://localhost:5173/?token=" + token;
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
