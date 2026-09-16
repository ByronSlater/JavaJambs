package com.javajambs.cher.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.image.ImageService;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import org.mockito.ArgumentCaptor;



@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ImageService imageService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, imageService);
    }

    @Test
    void registerUser_savesUserWithEncodedPasswordWhenUsernameIsFree() {
        when(userRepository.findByUsername("byron")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plaintext")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser("byron", "plaintext", "byron@example.com");

        assertThat(saved.getUsername()).isEqualTo("byron");
        assertThat(saved.getPassword()).isEqualTo("encoded-password");
        assertThat(saved.getEmail()).isEqualTo("byron@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_throwsWhenUsernameAlreadyExists() {
        when(userRepository.findByUsername("byron")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser("byron", "plaintext", "byron@example.com"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsername_returnsUserWhenFound() {
        User user = new User();
        user.setUsername("byron");
        when(userRepository.findByUsername("byron")).thenReturn(Optional.of(user));

        assertThat(userService.loadUserByUsername("byron")).isEqualTo(user);
    }

    @Test
    void loadUserByUsername_throwsWhenNotFound() {
        when(userRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void registerUser_allowsDuplicateEmailAcrossDifferentUsers() {
        when(userRepository.findByUsername("byron")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("jen")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User first = userService.registerUser("byron", "plaintext", "shared@example.com");
        User second = userService.registerUser("jen", "plaintext", "shared@example.com");

        assertThat(first.getEmail()).isEqualTo(second.getEmail());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void registerUser_treatsDifferentlyCasedUsernameAsAvailable() {
        when(userRepository.findByUsername("Byron")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plaintext")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.registerUser("Byron", "plaintext", "byron@example.com");

        assertThat(saved.getUsername()).isEqualTo("Byron");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_passesUsernameToRepositoryWithoutCaseNormalization() {
        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        when(userRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.registerUser("MixedCase", "plaintext", "mixed@example.com");

        verify(userRepository).findByUsername(usernameCaptor.capture());
        assertThat(usernameCaptor.getValue()).isEqualTo("MixedCase");
    }

    @Test
    void registerUser_whenUsernameAlreadyExists_doesNotEncodePasswordOrSave() {
        when(userRepository.findByUsername("byron")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.registerUser("byron", "plaintext", "byron@example.com"))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void loadUserByUsername_withNullUsername_throwsUsernameNotFoundException() {
        when(userRepository.findByUsername(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.loadUserByUsername(null))
                .isInstanceOf(UsernameNotFoundException.class);
    }
    @Test
    void updateProfile_savesEmailAndBioWithoutTouchingPictureWhenNoneProvided() throws Exception {
        User user = new User();
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.updateProfile(user, "byron@example.com", "hello", null);

        assertThat(saved.getEmail()).isEqualTo("byron@example.com");
        assertThat(saved.getBio()).isEqualTo("hello");
        assertThat(saved.getProfile_picture()).isNull();
        verify(imageService, never()).uploadImage(any(), any());
    }

    @Test
    void updateProfile_uploadsAndSetsProfilePictureWhenAnImageIsProvided() throws Exception {
        User user = new User();
        MultipartFile picture = new MockMultipartFile("profilePicture", "cat.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(eq("avatars"), eq(picture))).thenReturn("generated.png");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User saved = userService.updateProfile(user, "byron@example.com", "hello", picture);

        assertThat(saved.getProfile_picture()).isEqualTo("/img/avatars/generated.png");
    }

    @Test
    void updateProfile_rejectsNonImageFiles() {
        User user = new User();
        MultipartFile notAnImage = new MockMultipartFile("profilePicture", "resume.pdf", "application/pdf", "bytes".getBytes());

        assertThatIOException().isThrownBy(() -> userService.updateProfile(user, "byron@example.com", "hello", notAnImage));

        verify(userRepository, never()).save(any());
    }
}
