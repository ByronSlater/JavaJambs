package com.javajambs.cher.user;

import java.io.IOException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.image.ImageService;

@Service
public class UserService implements UserDetailsService {

    private static final String AVATARS_PATH = "avatars";

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ImageService imageService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, ImageService imageService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.imageService = imageService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "No user found with username: " + username
                        )
                );
    }

    public User registerUser(String username, String rawPassword, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new UsernameAlreadyExistsException(username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setEmail(email);

        return userRepository.save(user);
    }

    public User updateProfile(User user, String email, String bio, MultipartFile profilePicture) throws IOException {
        user.setEmail(email);
        user.setBio(bio);

        if (profilePicture != null && !profilePicture.isEmpty()) {
            String contentType = profilePicture.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IOException("Profile picture must be an image");
            }

            String filename = imageService.uploadImage(AVATARS_PATH, profilePicture);
            user.setProfile_picture("/img/%s/%s".formatted(AVATARS_PATH, filename));
        }

        return userRepository.save(user);
    }
}
