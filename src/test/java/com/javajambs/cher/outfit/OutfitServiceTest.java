package com.javajambs.cher.outfit;

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

import com.javajambs.cher.clothes.Clothes;
import com.javajambs.cher.clothes.ClothesRepository;
import com.javajambs.cher.image.ImageService;
import com.javajambs.cher.user.User;

@ExtendWith(MockitoExtension.class)
class OutfitServiceTest {

    @Mock
    private OutfitRepository outfitRepository;

    @Mock
    private ClothesRepository clothesRepository;

    @Mock
    private ImageService imageService;

    private OutfitService outfitService;

    @BeforeEach
    void setUp() {
        outfitService = new OutfitService(outfitRepository, clothesRepository, imageService);
    }

    @Test
    void getOutfits_returnsRepositoryResultsForUser() {
        User user = new User();
        List<Outfit> outfits = List.of(new Outfit("Beach look", user));
        when(outfitRepository.findByUser(user)).thenReturn(outfits);

        assertThat(outfitService.getOutfits(user)).isEqualTo(outfits);
    }

    @Test
    void getOutfitsByType_returnsRepositoryResultsForUserAndType() {
        User user = new User();
        List<Outfit> casualOutfits = List.of(new Outfit("Weekend look", user));
        when(outfitRepository.findByUserAndType(user, "casual")).thenReturn(casualOutfits);

        assertThat(outfitService.getOutfitsByType(user, "casual")).isEqualTo(casualOutfits);
    }

    @Test
    void getOutfitsByOutfits_returnsRepositoryResultsForUserAndOutfitName() {
        User user = new User();
        List<Outfit> matches = List.of(new Outfit("Beach look", user));
        when(outfitRepository.findByUserAndOutfitName(user, "Beach look")).thenReturn(matches);

        assertThat(outfitService.getOutfitsByOutfits(user, "Beach look")).isEqualTo(matches);
    }

    @Test
    void addOutfit_savesOutfitWithoutImageUrlWhenNoImageProvided() throws Exception {
        User user = new User();
        when(outfitRepository.save(any(Outfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Outfit saved = outfitService.addOutfit(user, "Beach look", null, null);

        assertThat(saved.getOutfitName()).isEqualTo("Beach look");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getImageUrl()).isNull();
        verify(imageService, never()).uploadImage(any(), any());
    }

    @Test
    void addOutfit_uploadsImageAndSetsImageUrlWhenImageProvided() throws Exception {
        User user = new User();
        MultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(eq("outfits"), eq(image))).thenReturn("generated.png");
        when(outfitRepository.save(any(Outfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Outfit saved = outfitService.addOutfit(user, "Beach look", null, image);

        assertThat(saved.getImageUrl()).isEqualTo("/image/outfits/generated.png");
    }

    @Test
    void addOutfit_rejectsNonImageFilesAndDoesNotSave() {
        User user = new User();
        MultipartFile notAnImage = new MockMultipartFile("image", "resume.pdf", "application/pdf", "bytes".getBytes());

        assertThatIOException().isThrownBy(() -> outfitService.addOutfit(user, "Beach look", null, notAnImage));

        verify(outfitRepository, never()).save(any());
    }

    @Test
    void addOutfit_resolvesOwnedClothesIntoOutfit() throws Exception {
        User user = new User();
        Clothes jacket = new Clothes("Denim jacket", user);
        when(clothesRepository.findAllById(List.of(1L))).thenReturn(List.of(jacket));
        when(outfitRepository.save(any(Outfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Outfit saved = outfitService.addOutfit(user, "Beach look", List.of(1L), null);

        assertThat(saved.getClothes()).containsExactly(jacket);
    }

    @Test
    void addOutfit_throwsAccessDeniedWhenClothesNotOwnedByUserAndDoesNotSave() {
        User user = new User();
        User someoneElse = new User();
        Clothes notMine = new Clothes("Denim jacket", someoneElse);
        when(clothesRepository.findAllById(List.of(1L))).thenReturn(List.of(notMine));

        assertThatThrownBy(() -> outfitService.addOutfit(user, "Beach look", List.of(1L), null))
                .isInstanceOf(AccessDeniedException.class);

        verify(outfitRepository, never()).save(any());
    }

    @Test
    void updateOutfit_updatesNameWhenOwnedByRequestingUser() throws Exception {
        User owner = new User();
        Outfit existing = new Outfit("Old look", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(outfitRepository.save(any(Outfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Outfit updated = outfitService.updateOutfit(1L, owner, "New look", null, null);

        assertThat(updated.getOutfitName()).isEqualTo("New look");
    }

    @Test
    void updateOutfit_throwsWhenOutfitNotFound() {
        when(outfitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outfitService.updateOutfit(1L, new User(), "New look", null, null))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void updateOutfit_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        Outfit existing = new Outfit("Old look", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> outfitService.updateOutfit(1L, other, "New look", null, null))
                .isInstanceOf(AccessDeniedException.class);

        verify(outfitRepository, never()).save(any());
    }

    @Test
    void updateOutfit_uploadsImageAndSetsImageUrlWhenImageProvided() throws Exception {
        User owner = new User();
        Outfit existing = new Outfit("Old look", owner);
        MultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", "bytes".getBytes());
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(imageService.uploadImage(eq("outfits"), eq(image))).thenReturn("generated.png");
        when(outfitRepository.save(any(Outfit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Outfit updated = outfitService.updateOutfit(1L, owner, "New look", null, image);

        assertThat(updated.getImageUrl()).isEqualTo("/image/outfits/generated.png");
    }

    @Test
    void updateOutfit_rejectsNonImageFilesAndDoesNotSave() {
        User owner = new User();
        Outfit existing = new Outfit("Old look", owner);
        MultipartFile notAnImage = new MockMultipartFile("image", "resume.pdf", "application/pdf", "bytes".getBytes());
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatIOException().isThrownBy(() -> outfitService.updateOutfit(1L, owner, "New look", null, notAnImage));

        verify(outfitRepository, never()).save(any());
    }

    @Test
    void deleteOutfit_deletesWhenOwnedByRequestingUser() throws Exception {
        User owner = new User();
        Outfit existing = new Outfit("Beach look", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        outfitService.deleteOutfit(1L, owner);

        verify(outfitRepository).delete(existing);
    }

    @Test
    void deleteOutfit_throwsWhenOutfitNotFound() {
        when(outfitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outfitService.deleteOutfit(1L, new User()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteOutfit_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        Outfit existing = new Outfit("Beach look", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> outfitService.deleteOutfit(1L, other))
                .isInstanceOf(AccessDeniedException.class);

        verify(outfitRepository, never()).delete(any());
    }

    @Test
    void getOutfitForEdit_returnsOutfitWhenOwnedByRequestingUser() throws Exception {
        User owner = new User();
        Outfit existing = new Outfit("Beach look", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThat(outfitService.getOutfitForEdit(1L, owner)).isEqualTo(existing);
    }

    @Test
    void getOutfitForEdit_throwsWhenOutfitNotFound() {
        when(outfitRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> outfitService.getOutfitForEdit(1L, new User()))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getOutfitForEdit_throwsAccessDeniedWhenRequestingUserIsNotOwner() {
        User owner = new User();
        User other = new User();
        Outfit existing = new Outfit("Beach look", owner);
        when(outfitRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> outfitService.getOutfitForEdit(1L, other))
                .isInstanceOf(AccessDeniedException.class);
    }
}
