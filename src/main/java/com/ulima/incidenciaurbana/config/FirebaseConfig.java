package com.ulima.incidenciaurbana.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream serviceAccount = openCredentials()) {
                if (serviceAccount == null) {
                    System.err.println("⚠️  WARNING: credenciales de Firebase no encontradas.");
                    System.err.println("⚠️  Las notificaciones Firebase NO estarán disponibles.");
                    return null;
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                System.out.println("✅ Firebase inicializado correctamente.");
                return FirebaseApp.initializeApp(options);
            } catch (IOException e) {
                System.err.println("⚠️  ERROR al inicializar Firebase: " + e.getMessage());
                System.err.println("⚠️  La aplicación continuará sin notificaciones push.");
                return null;
            }
        }
        return FirebaseApp.getInstance();
    }

    private InputStream openCredentials() throws IOException {
        String configuredPath = System.getenv().getOrDefault(
                "FIREBASE_CREDENTIALS_PATH",
                "/etc/secrets/firebase-service-account.json");
        Path secretPath = Path.of(configuredPath);

        if (Files.isRegularFile(secretPath)) {
            return Files.newInputStream(secretPath);
        }

        try {
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");
            return resource.exists() ? resource.getInputStream() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
