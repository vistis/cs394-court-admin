package kh.edu.paragoniu.court_admin.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kh.edu.paragoniu.court_shared.config.S3Config;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class StorageService {
    
    @Autowired
    private S3Client s3Client;

    @Autowired
    private S3Config s3Config;

    
    public String uploadFile(MultipartFile file, String folder) {
        try {
            String key = folder + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

            PutObjectRequest request = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .contentType(file.getContentType())
                .build();
            
            s3Client.putObject(
                request,
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to upload file to R2", e);
        }
    }

    private String getExtension(String filename) {
        if(filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    public static class StorageException extends RuntimeException {
        public StorageException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public String getFullUrl(String key) {
        
        if (key == null || key.isBlank()) {
            return null;
        }

        String base = s3Config.getPublicUrl();

        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String normalizeKey = key.startsWith("/") ? key.substring(1) : key;

        return base + "/" + normalizeKey;
    }

    public void deleteFile(String key) {
        if (key == null || key.isBlank()) {
            return;
        }

        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(key)
                .build();

            s3Client.deleteObject(request);
        } catch (Exception e){
            System.err.println("Fail to delete old profile picture: " + key + " - " + e.getMessage());
        }
    }
}
