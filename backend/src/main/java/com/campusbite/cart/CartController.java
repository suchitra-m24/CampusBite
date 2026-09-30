package com.campusbite.cart;

import com.campusbite.cart.dto.AddToCartRequest;
import com.campusbite.cart.dto.CartDto;
import com.campusbite.cart.dto.UpdateCartItemRequest;
import com.campusbite.common.security.SecurityUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartDto getCart(@AuthenticationPrincipal SecurityUser principal) {
        return cartService.getCart(principal.getUser());
    }

    @PostMapping("/items")
    public CartDto addItem(@AuthenticationPrincipal SecurityUser principal,
                            @Valid @RequestBody AddToCartRequest request) {
        return cartService.addItem(principal.getUser(), request);
    }

    @PatchMapping("/items/{cartItemId}")
    public CartDto updateItem(@AuthenticationPrincipal SecurityUser principal,
                               @PathVariable Long cartItemId,
                               @Valid @RequestBody UpdateCartItemRequest request) {
        return cartService.updateItemQuantity(principal.getUser(), cartItemId, request.quantity());
    }

    @DeleteMapping
    public CartDto clear(@AuthenticationPrincipal SecurityUser principal) {
        return cartService.clear(principal.getUser());
    }
}
