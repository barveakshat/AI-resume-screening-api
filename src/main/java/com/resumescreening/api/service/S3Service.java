package com.resumescreening.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    // Upload file to S3
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // Generate unique file name
        String fileName = generateFileName(file.getOriginalFilename(), folder);

        try {
            // Create put request
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .build();

            // Upload file
            s3Client.putObject(putRequest, RequestBody.fromInputStream(
                    file.getInputStream(),
                    file.getSize()
            ));

            log.info("File uploaded successfully: {}", fileName);

            // Return S3 URL
            return getFileUrl(fileName);

        } catch (S3Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    // Download file from S3
    public InputStream downloadFile(String fileKey) {
        try {
            GetObjectRequest getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            return s3Client.getObject(getRequest);

        } catch (S3Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    // Delete file from S3
    public void deleteFile(String fileKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.deleteObject(deleteRequest);
            log.info("File deleted successfully: {}", fileKey);

        } catch (S3Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    // Check if file exists
    public boolean fileExists(String fileKey) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(fileKey)
                    .build();

            s3Client.headObject(headRequest);
            return true;

        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence: {}", e.getMessage());
            return false;
        }
    }

    // Generate unique file name
    private String generateFileName(String originalFileName, String folder) {
        String extension = getFileExtension(originalFileName);
        String uuid = UUID.randomUUID().toString();
        return folder + "/" + uuid + extension;
    }

    // Extract file extension
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    // Get public URL for file
    private String getFileUrl(String fileKey) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, fileKey);
    }

    // Extract file key from URL
    public String extractFileKeyFromUrl(String fileUrl) {
        // URL format: https://bucket-name.s3.region.amazonaws.com/folder/file.pdf
        if (fileUrl.contains(bucketName)) {
            return fileUrl.substring(fileUrl.indexOf(bucketName) + bucketName.length() + 1);
        }
        return fileUrl;
    }
}