package vn.uth.firebasenotification.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.uth.firebasenotification.dto.NotifyDto;
import vn.uth.firebasenotification.dto.RegisterTokenDto;
import vn.uth.firebasenotification.dto.TokenDto;
import vn.uth.firebasenotification.service.FcmService;
import vn.uth.firebasenotification.service.UserService;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final FcmService fcmService;
    private final UserService userService;

    public NotificationController(FcmService fcmService, UserService userService) {
        this.fcmService = fcmService;
        this.userService = userService;
    }

    @PostMapping("/register-token")
    public ResponseEntity<?> registerToken(@RequestBody RegisterTokenDto dto) {
        UUID userId = userService.getMyInfo().getId();
        fcmService.registerToken(userId, dto.getToken(), dto.getPlatform(), dto.getDeviceInfo());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/token")
    public ResponseEntity<?> removeToken(@RequestBody TokenDto dto) {
        UUID userId = userService.getMyInfo().getId();
        fcmService.removeToken(userId, dto.getToken());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<?> notifyUser(@PathVariable UUID userId, @RequestBody NotifyDto dto) {
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
