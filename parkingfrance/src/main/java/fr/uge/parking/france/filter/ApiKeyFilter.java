package fr.uge.parking.france.filter;

import fr.uge.parking.france.dto.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY = "1234";
    private static final String HEADER_NAME = "x-api-key";

    @Override
    public void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var path = request.getRequestURI();
        if (path.startsWith("/admin")) {
            var apiKey = request.getHeader(HEADER_NAME);
            if (apiKey == null || !apiKey.equals(API_KEY)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write(new ErrorResponse(401, "Unauthorized: Invalid or missing API key").toString());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}
