package com.heimdall.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.RedirectStrategy;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TokenAuthSuccessHandlerTest {

    private JwtService jwtService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private Authentication authentication;
    private OAuth2User oAuth2User;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        authentication = mock(Authentication.class);
        oAuth2User = mock(OAuth2User.class);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getAttribute("email")).thenReturn("user@example.com");
        when(jwtService.generateToken("user@example.com")).thenReturn("mock-jwt-token");
    }

    @Test
    void testRedirectWithConfiguredFrontendUrl() throws Exception {
        TokenAuthSuccessHandler handler = new TokenAuthSuccessHandler(jwtService, "http://custom-frontend.example.com:3000");
        RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
        handler.setRedirectStrategy(redirectStrategy);

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());
        assertEquals("http://custom-frontend.example.com:3000/?token=mock-jwt-token", urlCaptor.getValue());
    }

    @Test
    void testRedirectWithTrailingSlashFrontendUrl() throws Exception {
        TokenAuthSuccessHandler handler = new TokenAuthSuccessHandler(jwtService, "http://custom-frontend.example.com:3000/");
        RedirectStrategy redirectStrategy = mock(RedirectStrategy.class);
        handler.setRedirectStrategy(redirectStrategy);

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(redirectStrategy).sendRedirect(eq(request), eq(response), urlCaptor.capture());
        assertEquals("http://custom-frontend.example.com:3000/?token=mock-jwt-token", urlCaptor.getValue());
    }
}
