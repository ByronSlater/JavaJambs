package com.javajambs.cher.clothes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.javajambs.cher.image.ImageService;
import com.javajambs.cher.user.User;

@ExtendWith(MockitoExtension.class)
class ClothesServiceTest {

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ImageService imageService;

    private ClothesService clothesService;

    @BeforeEach
    void setUp() {
        clothesService = new ClothesService(clothesRepository, imageService);
    }

    @Test
    void getWardrobe_returnsRepositoryResultsForUser() {
        User user = new User();
        List<Clothes> wardrobe = List.of(new Clothes("Denim jacket", user));
        when(clothesRepository.findByUser(user)).thenReturn(wardrobe);

        assertThat(clothesService.getWardrobe(user)).isEqualTo(wardrobe);
    }

    @Test
    void getWardrobeByType_returnsRepositoryResultsForUserAndType() {
        User user = new User();
        List<Clothes> jackets = List.of(new Clothes("Denim jacket", user));
        when(clothesRepository.findByUserAndType(user, "jacket")).thenReturn(jackets);

        assertThat(clothesService.getWardrobeByType(user, "jacket")).isEqualTo(jackets);
    }

    @Test
    void addClothingItem_savesClothesWithoutImageUrlWhenNoImageProvided() throws Exception {
        User user = new User();
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes saved = clothesService.addClothingItem(user, "Denim jacket", null, null, null);

        assertThat(saved.getName()).isEqualTo("Denim jacket");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getImageUrl()).isNull();
        verify(imageService, never()).uploadImage(any(), any());
    }

    @Test
    void addClothingItem_uploadsImageAndSetsImageUrlWhenImageProvided() throws Exception {
        User user = new User();
        MultipartFile image = new MockMultipartFile("image", "jacket.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(eq("clothes"), eq(image))).thenReturn("generated.png");
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes saved = clothesService.addClothingItem(user, "Denim jacket", null, image, null);

        assertThat(saved.getImageUrl()).isEqualTo("/img/clothes/generated.png");
    }

    @Test
    void addClothingItem_rejectsNonImageFilesAndDoesNotSave() {
        User user = new User();
        MultipartFile notAnImage = new MockMultipartFile("image", "resume.pdf", "application/pdf", "bytes".getBytes());

        assertThatIOException().isThrownBy(
                () -> clothesService.addClothingItem(user, "Denim jacket", null, notAnImage, null));

        verify(clothesRepository, never()).save(any());
    }

    @Test
    void addClothingItem_setsTypeWhenProvided() throws Exception {
        User user = new User();
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes saved = clothesService.addClothingItem(user, "Denim jacket", "top", null, null);

        assertThat(saved.getType()).isEqualTo("top");
    }

    @Test
    void addClothingItem_usesImageUrlDirectlyWhenNoFileProvided() throws Exception {
        User user = new User();
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes saved = clothesService.addClothingItem(user, "Denim jacket", null, null, "/img/clothes/edited.png");

        assertThat(saved.getImageUrl()).isEqualTo("/img/clothes/edited.png");
        verify(imageService, never()).uploadImage(any(), any());
    }

    @Test
    void addClothingItem_prefersFreshFileUploadOverImageUrlWhenBothProvided() throws Exception {
        User user = new User();
        MultipartFile image = new MockMultipartFile("image", "jacket.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(eq("clothes"), eq(image))).thenReturn("generated.png");
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes saved = clothesService.addClothingItem(
                user, "Denim jacket", null, image, "/img/clothes/edited.png");

        assertThat(saved.getImageUrl()).isEqualTo("/img/clothes/generated.png");
    }

    @Test
    void updateClothingItem_updatesNameWhenOwnedByRequestingUser() throws Exception {
        User owner = new User();
        Clothes existing = new Clothes("Old name", owner);
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes updated = clothesService.updateClothingItem(1L, owner, "New name", null);

        assertThat(updated.getName()).isEqualTo("New name");
    }

    @Test
    void updateClothingItem_throwsWhenClothesNotFound() {
        when(clothesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clothesService.updateClothingItem(1L, new User(), "New name", null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateClothingItem_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        Clothes existing = new Clothes("Old name", owner);
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> clothesService.updateClothingItem(1L, other, "New name", null))
                .isInstanceOf(AccessDeniedException.class);

        verify(clothesRepository, never()).save(any());
    }

    @Test
    void updateClothingItem_uploadsImageAndSetsImageUrlWhenImageProvided() throws Exception {
        User owner = new User();
        Clothes existing = new Clothes("Old name", owner);
        MultipartFile image = new MockMultipartFile("image", "jacket.png", "image/png", "bytes".getBytes());
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(imageService.uploadImage(eq("clothes"), eq(image))).thenReturn("generated.png");
        when(clothesRepository.save(any(Clothes.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Clothes updated = clothesService.updateClothingItem(1L, owner, "New name", image);

        assertThat(updated.getImageUrl()).isEqualTo("/img/clothes/generated.png");
    }

    @Test
    void updateClothingItem_rejectsNonImageFilesAndDoesNotSave() {
        User owner = new User();
        Clothes existing = new Clothes("Old name", owner);
        MultipartFile notAnImage = new MockMultipartFile("image", "resume.pdf", "application/pdf", "bytes".getBytes());
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatIOException().isThrownBy(() -> clothesService.updateClothingItem(1L, owner, "New name", notAnImage));

        verify(clothesRepository, never()).save(any());
    }

    @Test
    void deleteClothingItem_deletesWhenOwnedByRequestingUser() {
        User owner = new User();
        Clothes existing = new Clothes("Denim jacket", owner);
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));

        clothesService.deleteClothingItem(1L, owner);

        verify(clothesRepository).delete(existing);
    }

    @Test
    void deleteClothingItem_throwsWhenClothesNotFound() {
        when(clothesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clothesService.deleteClothingItem(1L, new User()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteClothingItem_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        Clothes existing = new Clothes("Denim jacket", owner);
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> clothesService.deleteClothingItem(1L, other))
                .isInstanceOf(AccessDeniedException.class);

        verify(clothesRepository, never()).delete(any());
    }

    @Test
    void getClothingItemForEdit_returnsClothesWhenOwnedByRequestingUser() {
        User owner = new User();
        Clothes existing = new Clothes("Denim jacket", owner);
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThat(clothesService.getClothingItemForEdit(1L, owner)).isEqualTo(existing);
    }

    @Test
    void getClothingItemForEdit_throwsWhenClothesNotFound() {
        when(clothesRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clothesService.getClothingItemForEdit(1L, new User()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getClothingItemForEdit_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        Clothes existing = new Clothes("Denim jacket", owner);
        when(clothesRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> clothesService.getClothingItemForEdit(1L, other))
                .isInstanceOf(AccessDeniedException.class);
    }
}
