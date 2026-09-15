package com.javajambs.cher.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
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
}
