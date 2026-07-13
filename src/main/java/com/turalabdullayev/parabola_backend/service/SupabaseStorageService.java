package com.turalabdullayev.parabola_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String supabaseAnonKey;

    @Value("${supabase.bucket-name}")
    private String bucketName;

    private final RestTemplate restTemplate = new RestTemplate();

    public String uploadFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fayl boş ola bilməz!");
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Yalnız şəkil formatında fayl yükləyə bilərsiniz!");
        }

        // Generate unique filename
        String originalFileName = file.getOriginalFilename();
        String fileExtension = ".jpg";
        if (originalFileName != null && originalFileName.contains(".")) {
            String ext = originalFileName.substring(originalFileName.lastIndexOf(".")).toLowerCase();
            if (ext.equals(".png") || ext.equals(".jpeg") || ext.equals(".jpg") || ext.equals(".webp") || ext.equals(".gif")) {
                fileExtension = ext;
            }
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

        // Construct Supabase Storage API endpoint
        String uploadUrl = String.format("%s/storage/v1/object/%s/%s", supabaseUrl, bucketName, uniqueFileName);

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseAnonKey);
        headers.set("apikey", supabaseAnonKey);
        headers.setContentType(MediaType.parseMediaType(contentType));

        HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);

        // Execute POST request to upload file
        ResponseEntity<String> response = restTemplate.exchange(
                uploadUrl,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            // Construct the public URL of the uploaded image
            return String.format("%s/storage/v1/object/public/%s/%s", supabaseUrl, bucketName, uniqueFileName);
        } else {
            throw new IOException("Şəkil Supabase-ə yüklənərkən xəta baş verdi: " + response.getBody());
        }
    }
}
