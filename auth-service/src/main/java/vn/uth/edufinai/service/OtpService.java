package vn.uth.edufinai.service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;

@Service
public class OtpService {
    private final Map<String, OtpData> otpCache = new ConcurrentHashMap<>();
    private static final long OTP_VALID_DURATION = 5 * 60 * 1000; // 5 minutes

    @Getter
    @Setter
    private static class OtpData {
        private String otp;
        private long expiryTime;

        public OtpData(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }
    }

    public String generateOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpCache.put(email, new OtpData(otp, System.currentTimeMillis() + OTP_VALID_DURATION));
        return otp;
    }

    public boolean validateOtp(String email, String otp) {
        OtpData otpData = otpCache.get(email);
        if (otpData == null) {
            return false;
        }
        if (System.currentTimeMillis() > otpData.getExpiryTime()) {
            otpCache.remove(email);
            return false;
        }
        if (otpData.getOtp().equals(otp)) {
            return true;
        }
        return false;
    }

    public void clearOtp(String email) {
        otpCache.remove(email);
    }
}
