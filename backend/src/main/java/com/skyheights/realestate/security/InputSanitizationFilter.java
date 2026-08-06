package com.skyheights.realestate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InputSanitizationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request) {
            @Override
            public String getParameter(String name) {
                String value = super.getParameter(name);
                return sanitize(value);
            }

            @Override
            public String[] getParameterValues(String name) {
                String[] values = super.getParameterValues(name);
                if (values == null) return null;
                for (int i = 0; i < values.length; i++) {
                    values[i] = sanitize(values[i]);
                }
                return values;
            }

            private String sanitize(String input) {
                if (input == null) return null;
                return input.replaceAll("(?i)<script.*?>.*?</script>", "")
                        .replaceAll("(?i)javascript:", "")
                        .trim();
            }
        };

        filterChain.doFilter(wrappedRequest, response);
    }
}
