package vn.uth.firebasenotification.config;

import com.google.api.client.util.Value;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${fcm.service-account-file:}")
    private String serviceAccountFile;

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount;
            if (!serviceAccountFile.isBlank()) {
                serviceAccount = new FileInputStream(serviceAccountFile);
            } else {
                // Fallback: use GOOGLE_APPLICATION_CREDENTIALS from env
                serviceAccount = null;
            }

            FirebaseOptions.Builder builder = FirebaseOptions.builder();
            if (serviceAccount != null) {
                builder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
            } else {
                builder.setCredentials(GoogleCredentials.getApplicationDefault());
            }

            FirebaseOptions options = builder.build();
            FirebaseApp.initializeApp(options);
        }
    }
}

