package com.javajambs.cher.image;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Tries to upload an image into an instance folder, returns the
     * generated filename of the uploaded image
     */
    public String uploadImage(
            String path,
            MultipartFile file) throws IOException {
        Path uploadsRoot = resolveUploadsRoot();
        Path absolute = uploadsRoot.resolve(path).normalize();

        if (!absolute.startsWith(uploadsRoot)) {
            throw new IOException("Invalid image path: " + path);
        }

        File baseDir = absolute.toFile();
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.lastIndexOf(".") == -1) {
            throw new IOException("Uploaded file must have an extension");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        String uniqueFilename = UUID.randomUUID().toString() + extension;

        Path targetPath = absolute.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetPath);

        return uniqueFilename;
    }

    public Resource loadImage(String path, String filename) throws IOException {
        Path uploadsRoot = resolveUploadsRoot();
        Path baseDir = uploadsRoot.resolve(path).normalize();
        Path targetPath = baseDir.resolve(filename).normalize();

        if (!baseDir.startsWith(uploadsRoot) || !targetPath.startsWith(baseDir)) {
            throw new IOException("Invalid image path: " + path + "/" + filename);
        }

        Resource resource = new UrlResource(targetPath.toUri());
        if (!resource.exists() || !resource.isReadable()) {
            throw new IOException("Could not read image: " + filename);
        }

        return resource;
    }

    private Path resolveUploadsRoot() {
        return Paths.get(System.getProperty("user.dir")).resolve(uploadDir).normalize();
    }
}
