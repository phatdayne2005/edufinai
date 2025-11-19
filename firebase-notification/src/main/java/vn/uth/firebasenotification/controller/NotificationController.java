package vn.uth.firebasenotification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.uth.firebasenotification.service.FcmService;

import java.security.Principal;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final FcmService fcmService;

    public NotificationController(FcmService fcmService) {
        this.fcmService = fcmService;
    }

    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(@RequestBody RegisterTokenDto dto, Principal p) {
        Long userId = getUserIdFromPrincipal(p); // or accept userId in DTO if called by other services with auth
        fcmService.registerToken(userId, dto.getToken(), dto.getPlatform(), dto.getDeviceInfo());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/token")
    public ResponseEntity<?> removeToken(@RequestBody TokenDto dto, Principal p) {
        Long userId = getUserIdFromPrincipal(p);
        fcmService.removeToken(userId, dto.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> notifyUser(@PathVariable Long userId, @RequestBody NotifyDto dto) {
        fcmService.sendToUser(userId, dto.getTitle(), dto.getBody(), dto.getData());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/topic/{topic}")
    public ResponseEntity<?> notifyTopic(@PathVariable String topic, @RequestBody NotifyDto dto) {
        fcmService.sendToTopic(topic, dto.getTitle(), dto.getBody(), dto.getData());
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcast(@RequestBody NotifyDto dto) {
        fcmService.broadcastToAll(dto.getTitle(), dto.getBody(), dto.getData());
        return ResponseEntity.accepted().build();
    }
}

