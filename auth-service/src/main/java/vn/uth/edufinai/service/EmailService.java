package vn.uth.edufinai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendOtpEmail(String to, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("EduFinAI - Mã xác thực OTP");
            message.setText("Mã OTP của bạn là: " + otp + "\nMã này sẽ hết hạn sau 5 phút.");
            
            mailSender.send(message);
            log.info("OTP email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", to, e);
            throw new vn.uth.edufinai.exception.AppException(vn.uth.edufinai.exception.ErrorCode.EMAIL_SEND_FAILURE);
        }
    }
}
