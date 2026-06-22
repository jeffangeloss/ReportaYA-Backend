package com.ulima.incidenciaurbana.service.impl;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.firebase.cloud.StorageClient;
import com.ulima.incidenciaurbana.service.IFirebaseStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class FirebaseStorageServiceImpl implements IFirebaseStorageService {

    private final String defaultBucket;

    public FirebaseStorageServiceImpl(
            @Value("${firebase.storage.bucket:}") String bucket) {
        this.defaultBucket = bucket;
    }

    @Override
    public String uploadFile(String fileName, byte[] fileBytes, String bucket) {
        String targetBucket = (bucket != null && !bucket.isEmpty()) ? bucket : defaultBucket;

        if (targetBucket.isEmpty()) {
            throw new RuntimeException("Firebase Storage bucket no configurado. Agrega FIREBASE_STORAGE_BUCKET en application-local.properties");
        }

        try {
            Storage storage = StorageClient.getInstance().bucket(targetBucket).getStorage();

            BlobId blobId = BlobId.of(targetBucket, fileName);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType("application/octet-stream")
                    .build();

            storage.create(blobInfo, fileBytes);

            return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    targetBucket,
                    fileName.replace("/", "%2F"));
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a Firebase Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadFile(String fileName, byte[] fileBytes) {
        return uploadFile(fileName, fileBytes, null);
    }

    @Override
    public void deleteFile(String fileName, String bucket) {
        String targetBucket = (bucket != null && !bucket.isEmpty()) ? bucket : defaultBucket;

        try {
            Storage storage = StorageClient.getInstance().bucket(targetBucket).getStorage();
            BlobId blobId = BlobId.of(targetBucket, fileName);
            storage.delete(blobId);
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de Firebase Storage: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        deleteFile(fileName, null);
    }

    @Override
    public boolean isAvailable() {
        if (defaultBucket.isEmpty()) return false;
        try {
            StorageClient.getInstance().bucket(defaultBucket);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
