package com.campusbite.menu;

import com.campusbite.menu.dto.AvailabilityRequest;
import com.campusbite.menu.dto.CategoryDto;
import com.campusbite.menu.dto.CategoryRequest;
import com.campusbite.menu.dto.MenuItemDto;
import com.campusbite.menu.dto.MenuItemRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MenuController {

    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    // ---- Public / student-facing reads ----
    @GetMapping("/api/menu/items")
    public ResponseEntity<List<MenuItemDto>> getAvailableItems() {
        return ResponseEntity.ok(menuService.getStudentMenuItems());
    }

    @GetMapping("/api/menu/items/specials")
    public ResponseEntity<List<MenuItemDto>> getSpecials() {
        return ResponseEntity.ok(menuService.getDailySpecials());
    }

    @GetMapping("/api/menu/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        return ResponseEntity.ok(menuService.getAllCategories());
    }

    // ---- Admin management ----

    @GetMapping("/api/admin/menu/items")
    public ResponseEntity<List<MenuItemDto>> getAllItemsForAdmin() {
        return ResponseEntity.ok(menuService.getAllItems());
    }

    @PostMapping("/api/admin/menu/categories")
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(menuService.createCategory(request));
    }

    @PutMapping("/api/admin/menu/categories/{categoryId}")
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Long categoryId,
                                                        @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(menuService.updateCategory(categoryId, request));
    }

    @DeleteMapping("/api/admin/menu/categories/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        menuService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/admin/menu/items")
    public ResponseEntity<MenuItemDto> createItem(@Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.createItem(request));
    }

    @PutMapping("/api/admin/menu/items/{itemId}")
    public ResponseEntity<MenuItemDto> updateItem(@PathVariable Long itemId,
                                                    @Valid @RequestBody MenuItemRequest request) {
        return ResponseEntity.ok(menuService.updateItem(itemId, request));
    }

    @DeleteMapping("/api/admin/menu/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        menuService.deleteItem(itemId);
        return ResponseEntity.noContent().build();
    }

    // ---- Staff / admin: instant stock toggle ----

    @GetMapping("/api/staff/menu/items")
    public ResponseEntity<List<MenuItemDto>> getAllItemsForStaff() {
        // Deliberately unfiltered (unlike /api/menu/items) -- staff need to
        // manage stock on every item regardless of meal-time gating.
        return ResponseEntity.ok(menuService.getAllItems());
    }

    @PatchMapping("/api/staff/menu/items/{itemId}/availability")
    public ResponseEntity<MenuItemDto> setAvailability(@PathVariable Long itemId,
                                                          @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(menuService.setAvailability(itemId, request.available()));
    }
}
