package fr.uge.project.ruge.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                        HttpServletResponse response, 
                                        AuthenticationException exception) throws IOException {
        var targetUrl = AuthenticationFailureRedirects.resolveFormFailureUrl(exception);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
