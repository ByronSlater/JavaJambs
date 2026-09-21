package com.javajambs.cher.image;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;

@Service
public class ImageService {

    private final BlobContainerClient containerClient;

    @Autowired
    public ImageService(
            @Value("${azure.storage.blob.account-name}") String accountName,
            @Value("${azure.storage.blob.container-name}") String containerName,
            @Value("${azure.storage.blob.connection-string:}") String connectionString) {

        BlobContainerClientBuilder builder = new BlobContainerClientBuilder()
                .containerName(containerName);

        if (!connectionString.isBlank()) {
            builder.connectionString(connectionString);
        } else {
            builder.endpoint("https://" + accountName + ".blob.core.windows.net")
                    .credential(new DefaultAzureCredentialBuilder().build());
        }

        this.containerClient = builder.buildClient();
    }

    ImageService(BlobContainerClient containerClient) {
        this.containerClient = containerClient;
    }

    private static void requireNoPathTraversal(String segment, String label) throws IOException {
        if (segment == null || segment.isBlank()
                || segment.contains("..") || segment.startsWith("/") || segment.contains("\\")) {
            throw new IOException("Invalid " + label + ": " + segment);
        }
    }

    public String uploadImage(
            String path,
            MultipartFile file) throws IOException {

        requireNoPathTraversal(path, "path");

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null ||
                originalFilename.lastIndexOf(".") == -1) {
            throw new IOException("Uploaded file must have an extension");
        }

        String extension =
                originalFilename.substring(originalFilename.lastIndexOf("."));

        String uniqueFilename =
                UUID.randomUUID() + extension;

        String blobName = path + "/" + uniqueFilename;

        BlobClient blobClient =
                containerClient.getBlobClient(blobName);

        blobClient.upload(
                file.getInputStream(),
                file.getSize(),
                true
        );

        String contentType = file.getContentType();

        if (contentType != null) {
            blobClient.setHttpHeaders(
                    new BlobHttpHeaders()
                            .setContentType(contentType)
            );
        }

        return uniqueFilename;
    }

    public Resource loadImage(
            String path,
            String filename) throws IOException {

        requireNoPathTraversal(path, "path");
        requireNoPathTraversal(filename, "filename");

        String blobName = path + "/" + filename;

        BlobClient blobClient =
                containerClient.getBlobClient(blobName);

        if (!blobClient.exists()) {
            throw new IOException("Could not read image: " + filename);
        }

        byte[] data = blobClient.downloadContent().toBytes();

        return new ByteArrayResource(data);
    }
}