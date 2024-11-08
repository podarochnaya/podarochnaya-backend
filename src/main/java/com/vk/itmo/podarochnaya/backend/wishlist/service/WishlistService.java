package com.vk.itmo.podarochnaya.backend.wishlist.service;

import com.vk.itmo.podarochnaya.backend.exception.AccessDeniedRuntimeException;
import com.vk.itmo.podarochnaya.backend.exception.NotFoundException;
import com.vk.itmo.podarochnaya.backend.user.service.UserService;
import com.vk.itmo.podarochnaya.backend.wishlist.dto.FileDto;
import com.vk.itmo.podarochnaya.backend.wishlist.dto.Wishlist;
import com.vk.itmo.podarochnaya.backend.wishlist.dto.WishlistCreateRequest;
import com.vk.itmo.podarochnaya.backend.wishlist.dto.WishlistUpdateRequest;
import com.vk.itmo.podarochnaya.backend.wishlist.jpa.WishlistEntity;
import com.vk.itmo.podarochnaya.backend.wishlist.jpa.WishlistRepository;
import com.vk.itmo.podarochnaya.backend.wishlist.jpa.WishlistStatus;
import com.vk.itmo.podarochnaya.backend.wishlist.jpa.WishlistVisibility;
import com.vk.itmo.podarochnaya.backend.wishlist.mapper.WishlistMapper;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class WishlistService {
    public static final byte[] EMPTY_BYTES = new byte[0];
    private final WishlistRepository wishlistRepository;
    private final GiftService giftService;
    private final UserService userService;
    private final WishlistMapper mapper;
    private final PlatformTransactionManager transactionManager;

    @Transactional
    public Wishlist createWishlist(WishlistCreateRequest wishlistCreateRequest) {
        var tx = new TransactionTemplate(transactionManager);

        var currentUser = userService.getAuthenticatedUser();

        WishlistEntity wishlist = new WishlistEntity();
        wishlist.setTitle(wishlistCreateRequest.getTitle().trim());
        wishlist.setDescription(wishlistCreateRequest.getDescription().trim());
        wishlist.setStatus(WishlistStatus.OPENED);
        wishlist.setOwner(currentUser);
        wishlist.setVisibility(Objects.requireNonNullElse(wishlistCreateRequest.getVisibility(), WishlistVisibility.PUBLIC));
        wishlist.setAllowedUsers(new HashSet<>(userService.getByEmails(wishlistCreateRequest.getAllowedUserEmails())));

        WishlistEntity wishlistEntity = tx.execute(status -> wishlistRepository.save(wishlist));

        var wishlistId = wishlistEntity.getId();

        if (CollectionUtils.isNotEmpty(wishlistCreateRequest.getGifts())) {
            wishlistCreateRequest.getGifts()
                .forEach(gift -> {
                    var fileBase64 = gift.getFile();

                    var bytes = Optional.ofNullable(fileBase64.getFileContent())
                        .map(it ->
                            it.contains(",")
                                ? Arrays.stream(it.split(",")).reduce((first, second) -> second).orElse(null)
                                : it
                        )
                        .map(it -> Base64.getDecoder().decode(it))
                        .orElse(EMPTY_BYTES);

                    giftService.createGift(
                        wishlistId,
                        gift,
                        new FileDto(
                            fileBase64.getFileName(),
                            bytes,
                            fileBase64.getContentType()
                        )
                    );
                });
        }

        wishlistEntity = tx.execute(status -> wishlistRepository.findById(wishlistId).orElseThrow());

        return mapper.toWishlist(wishlistEntity);
    }

    @Transactional(readOnly = true)
    public List<Wishlist> getAllWishlists() {
        var currentUser = userService.getAuthenticatedUser();

        return mapper.toWishlists(wishlistRepository.findAllAccessibleWishlists(currentUser.getId()));
    }

    @Transactional
    public Wishlist updateWishlist(Long wishlistId, WishlistUpdateRequest wishlistUpdateRequest) {
        WishlistEntity wishlist = getWishlistById(wishlistId);

        checkOwner(wishlist);

        if (wishlistUpdateRequest.getTitle() != null) {
            wishlist.setTitle(wishlistUpdateRequest.getTitle().trim());
        }

        if (wishlistUpdateRequest.getDescription() != null) {
            wishlist.setDescription(wishlistUpdateRequest.getDescription().trim());
        }

        if (wishlistUpdateRequest.getStatus() != null) {
            wishlist.setStatus(wishlistUpdateRequest.getStatus());
        }

        if (wishlistUpdateRequest.getVisibility() != null) {
            wishlist.setVisibility(wishlistUpdateRequest.getVisibility());
        }

        if (wishlistUpdateRequest.getAllowedUserEmails() != null) {
            wishlist.setAllowedUsers(
                new HashSet<>(
                    userService.getByEmails(wishlistUpdateRequest.getAllowedUserEmails())
                )
            );
        }

        return mapper.toWishlist(wishlistRepository.save(wishlist));
    }

    private void checkOwner(WishlistEntity wishlist) {
        var currentUser = userService.getAuthenticatedUser();

        if (!Objects.equals(wishlist.getOwner().getId(), currentUser.getId())) {
            throw new AccessDeniedRuntimeException(currentUser.getEmail() + " is not the owner of wishlist " + wishlist.getId());
        }
    }

    public List<WishlistEntity> getWishlistsByIds(List<Long> wishlistIds) {
        var currentUser = userService.getAuthenticatedUser();

        return wishlistRepository.findAccessibleWishlistsByIds(wishlistIds, currentUser.getId());
    }

    public WishlistEntity getWishlistById(Long wishlistId) {
        var currentUser = userService.getAuthenticatedUser();

        return wishlistRepository.findAccessibleWishlistsByIds(List.of(wishlistId), currentUser.getId()).stream().findFirst()
            .orElseThrow(() -> new NotFoundException("Cannot find or forbidden access to wishlist with ID: " + wishlistId));
    }

    public boolean deleteWishlist(Long wishlistId) {
        Optional<WishlistEntity> existingWishlist = wishlistRepository.findById(wishlistId);

        if (existingWishlist.isPresent()) {
            checkOwner(existingWishlist.get());

            wishlistRepository.deleteById(wishlistId);

            return true;
        }
        return false;
    }
}
