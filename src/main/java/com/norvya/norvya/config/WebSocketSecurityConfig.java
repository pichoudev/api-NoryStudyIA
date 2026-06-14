package com.norvya.norvya.config;

import com.norvya.norvya.security.JwtAuthFilter;
import com.norvya.norvya.service.auth.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService         jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketSecurityConfig(JwtService jwtService,
                                   UserDetailsService userDetailsService) {
        this.jwtService         = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message,
                                      MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null
                        && StompCommand.CONNECT.equals(accessor.getCommand())) {

                    String authHeader = accessor
                            .getFirstNativeHeader("Authorization");

                    if (authHeader != null
                            && authHeader.startsWith("Bearer ")) {

                        String token = authHeader.substring(7);

                        if (jwtService.isTokenValid(token)) {
                            String email = jwtService.extractEmail(token);
                            UserDetails userDetails =
                                    userDetailsService.loadUserByUsername(email);

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(
                                            userDetails, null,
                                            userDetails.getAuthorities()
                                    );

                            accessor.setUser(auth);
                            log.info("WebSocket authentifié : {}", email);
                        } else {
                            log.warn("WebSocket — token invalide");
                        }
                    }
                }
                return message;
            }
        });
    }
}