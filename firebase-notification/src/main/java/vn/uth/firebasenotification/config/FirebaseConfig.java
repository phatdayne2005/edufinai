package vn.uth.firebasenotification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @Value("${fcm.service-account-file:}")
    private String serviceAccountFile;
    private final ResourceLoader resourceLoader;

    public FirebaseConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void init() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            InputStream serviceAccount = null;
            if (!serviceAccountFile.isBlank()) {
                Resource resource = resourceLoader.getResource(serviceAccountFile);
                if (resource.exists()) {
                    serviceAccount = resource.getInputStream();
                } else {
                    throw new IOException("Service account file not found: " + serviceAccountFile);
                }
            }
            // Fallback: use GOOGLE_APPLICATION_CREDENTIALS from env

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

