package com.vk.itmo.podarochnaya.backend.wishlist.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class GiftCreateRequest extends GiftCreateBaseRequest {
    @NotNull(message = "Wishlist ID is required")
    private Long wishlistId;
}
