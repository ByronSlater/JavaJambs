package com.javajambs.cher.image;

import java.io.IOException;
import java.nio.file.Files;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/img")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping("/upload")
    @ResponseBody
    public String uploadImage(
            @RequestParam("image") MultipartFile file,
            @RequestParam(value = "path", defaultValue = "misc") String path) throws IOException {
        String filename = imageService.uploadImage(path, file);
        String url = "/img/%s/%s".formatted(path, filename);

        return "<img src=\"%s\" alt=\"Uploaded image\" />".formatted(url);
    }

    @GetMapping("/{path}/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getImage(
            @PathVariable String path,
            @PathVariable String filename) throws IOException {
        Resource resource = imageService.loadImage(path, filename);
        String contentType = Files.probeContentType(resource.getFile().toPath());

        return ResponseEntity.ok()
                .contentType(contentType != null
                        ? MediaType.parseMediaType(contentType)
                        : MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
