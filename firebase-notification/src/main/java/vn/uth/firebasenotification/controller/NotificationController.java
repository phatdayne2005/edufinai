package vn.uth.firebasenotification.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.uth.firebasenotification.dto.NotifyDto;
import vn.uth.firebasenotification.dto.RegisterTokenDto;
import vn.uth.firebasenotification.dto.TokenDto;
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

    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated user");
        }
        try {
            return Long.valueOf(principal.getName());
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user id in principal");
        }
    }
}
