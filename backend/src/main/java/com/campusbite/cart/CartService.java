package com.campusbite.cart;

import com.campusbite.cart.dto.AddToCartRequest;
import com.campusbite.cart.dto.CartDto;
import com.campusbite.common.exception.BadRequestException;
import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.menu.MenuItem;
import com.campusbite.menu.MenuItemRepository;
import com.campusbite.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final MenuItemRepository menuItemRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                        MenuItemRepository menuItemRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.menuItemRepository = menuItemRepository;
    }

    @Transactional
    public CartDto getCart(User student) {
        return CartDto.from(getOrCreateCart(student));
    }

    @Transactional
    public CartDto addItem(User student, AddToCartRequest request) {
        Cart cart = getOrCreateCart(student);
        MenuItem menuItem = menuItemRepository.findById(request.menuItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));

        if (!menuItem.isAvailable()) {
            throw new BadRequestException(menuItem.getName() + " is currently sold out");
        }

        java.util.List<String> requestedCustomizations = request.selectedCustomizations() == null
                ? java.util.List.of() : request.selectedCustomizations();
        CartItem existing = cart.getItems().stream()
                .filter(ci -> ci.getMenuItem().getId().equals(menuItem.getId())
                        && new java.util.ArrayList<>(ci.getSelectedCustomizations()).equals(requestedCustomizations))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.quantity());
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setMenuItem(menuItem);
            item.setQuantity(request.quantity());
            item.setSelectedCustomizations(request.selectedCustomizations() == null ? java.util.List.of() : request.selectedCustomizations());
            cart.getItems().add(item);
        }

        cartRepository.save(cart);
        return CartDto.from(cart);
    }

    @Transactional
    public CartDto updateItemQuantity(User student, Long cartItemId, int quantity) {
        Cart cart = getOrCreateCart(student);
        CartItem item = cart.getItems().stream()
                .filter(ci -> ci.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (quantity <= 0) {
            cart.getItems().remove(item);
        } else {
            item.setQuantity(quantity);
        }
        cartRepository.save(cart);
        return CartDto.from(cart);
    }

    @Transactional
    public CartDto clear(User student) {
        Cart cart = getOrCreateCart(student);
        cart.getItems().clear();
        cartRepository.save(cart);
        return CartDto.from(cart);
    }

    @Transactional
    public Cart getOrCreateCart(User student) {
        return cartRepository.findByStudent(student).orElseGet(() -> cartRepository.save(new Cart(student)));
    }
}
