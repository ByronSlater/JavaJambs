package com.javajambs.cher.post;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.image.ImageService;
import com.javajambs.cher.user.User;

@Controller
public class PostController {

    private static final String IMAGES_PATH = "posts";

    private final PostRepository repository;
    private final ImageService imageService;

    public PostController(PostRepository repository, ImageService imageService) {
        this.repository = repository;
        this.imageService = imageService;
    }

    @GetMapping("/posts")
    public String index(@AuthenticationPrincipal User user, Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        Iterable<Post> posts = repository.findAll();
        model.addAttribute("posts", posts);
        model.addAttribute("post", new Post());
        return "posts";
    }

    @PostMapping("/posts")
    public String create(
            @ModelAttribute Post post,
            @RequestParam(required = false) MultipartFile image,
            @AuthenticationPrincipal User user,
            Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        post.setUser(user);
        post.setCreatedAt(LocalDateTime.now());

        try {
            attachImage(post, image);
        } catch (IOException e) {
            model.addAttribute("posts", repository.findAll());
            model.addAttribute("post", post);
            model.addAttribute("postError", "Sorry! Couldn't upload that image. Please try again.");
            return "posts";
        }

        repository.save(post);

        return "redirect:/posts";
    }

    @GetMapping("/posts/{id}/edit")
    public String edit(@PathVariable Long id, @AuthenticationPrincipal User user, Model model) {

        if (user == null) {
            return "redirect:/login";
        }

        Post post = repository.findById(id)
                .orElseThrow();

        model.addAttribute("post", post);

        return "edit-posts";
    }

    @PostMapping("/posts/{id}/edit")
    public String update(
            @PathVariable Long id,
            @ModelAttribute Post post,
            @RequestParam(required = false) MultipartFile image,
            Model model) {

        Post existingPost = repository.findById(id)
                .orElseThrow();

        existingPost.setCaption(post.getCaption());

        try {
            attachImage(existingPost, image);
        } catch (IOException e) {
            model.addAttribute("post", existingPost);
            model.addAttribute("postError", "Sorry! Couldn't upload that image. Please try again.");
            return "edit-posts";
        }

        repository.save(existingPost);

        return "redirect:/posts";
    }

    @PostMapping("/posts/{id}/delete")
    public String delete(@PathVariable Long id) {

        Post post = repository.findById(id)
                .orElseThrow();

        repository.delete(post);

        return "redirect:/posts";
    }

    private void attachImage(Post post, MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            return;
        }

        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IOException("Post image must be an image");
        }

        String filename = imageService.uploadImage(IMAGES_PATH, image);
        post.setImageUrl("/img/%s/%s".formatted(IMAGES_PATH, filename));
    }
}
