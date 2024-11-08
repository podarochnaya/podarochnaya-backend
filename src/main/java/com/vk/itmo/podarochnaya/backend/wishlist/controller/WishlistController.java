package com.vk.itmo.podarochnaya.backend.wishlist.controller;

import com.vk.itmo.podarochnaya.backend.wishlist.dto.Wishlist;
import com.vk.itmo.podarochnaya.backend.wishlist.dto.WishlistCreateRequest;
import com.vk.itmo.podarochnaya.backend.wishlist.dto.WishlistUpdateRequest;
import com.vk.itmo.podarochnaya.backend.wishlist.mapper.WishlistMapper;
import com.vk.itmo.podarochnaya.backend.wishlist.service.WishlistService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;
    private final WishlistMapper wishlistMapper;

    @PostMapping
    public ResponseEntity<Wishlist> createWishlist(@Valid @RequestBody WishlistCreateRequest wishlistCreateRequest) {
        Wishlist createdWishlist = wishlistService.createWishlist(wishlistCreateRequest);
        return new ResponseEntity<>(createdWishlist, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Wishlist>> getAllWishlists() {
        List<Wishlist> wishlists = wishlistService.getAllWishlists();
        return new ResponseEntity<>(wishlists, HttpStatus.OK);
    }

    @GetMapping("/{wishlistId}")
    public ResponseEntity<Wishlist> getWishlistById(@PathVariable Long wishlistId) {
        Wishlist wishlist = wishlistMapper.toWishlist(wishlistService.getWishlistById(wishlistId));
        return wishlist != null ? new ResponseEntity<>(wishlist, HttpStatus.OK)
            : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{wishlistId}")
    public ResponseEntity<Wishlist> updateWishlist(@PathVariable Long wishlistId,
                                                   @Valid @RequestBody WishlistUpdateRequest wishlistUpdateRequest) {
        Wishlist updatedWishlist = wishlistService.updateWishlist(wishlistId, wishlistUpdateRequest);
        return updatedWishlist != null ? new ResponseEntity<>(updatedWishlist, HttpStatus.OK)
            : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{wishlistId}")
    public ResponseEntity<Void> deleteWishlist(@PathVariable Long wishlistId) {
        boolean isDeleted = wishlistService.deleteWishlist(wishlistId);
        return isDeleted ? new ResponseEntity<>(HttpStatus.NO_CONTENT)
            : new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
}
