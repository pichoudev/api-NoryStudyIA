package com.norvya.norvya.controller;

import com.norvya.norvya.service.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final NotificationService notificationService;

    // ── Ping / Pong ────────────────────────────────────────
    // Client : /app/ping
    // Reçoit : /user/queue/pong
    @MessageMapping("/ping")
    @SendToUser("/queue/pong")
    public Map<String, String> ping(Principal principal) {
        log.debug("Ping reçu de : {}", principal.getName());
        Map<String, String> response = new HashMap<>();
        response.put("type",      "PONG");
        response.put("message",   "pong");
        response.put("user",      principal.getName());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    // ── Abonnement aux notifications ───────────────────────
    // Client : /app/subscribe
    // Reçoit : /user/queue/notifications
    @MessageMapping("/subscribe")
    @SendToUser("/queue/notifications")
    public Map<String, String> subscribe(Principal principal) {
        log.info("Utilisateur connecté au WebSocket : {}",
                principal.getName());
        Map<String, String> response = new HashMap<>();
        response.put("type",      "SUBSCRIBED");
        response.put("message",   "Connecté aux notifications en temps réel");
        response.put("user",      principal.getName());
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}