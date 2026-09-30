package com.campusbite.menu;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalTime;
import java.time.ZoneId;
import com.campusbite.common.exception.ResourceNotFoundException;
import com.campusbite.common.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import com.campusbite.common.ws.BroadcastService;
import com.campusbite.menu.dto.CategoryDto;
import com.campusbite.menu.dto.CategoryRequest;
import com.campusbite.menu.dto.MenuItemDto;
import com.campusbite.menu.dto.MenuItemRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MenuService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final BroadcastService broadcastService;
    private final ZoneId campusZone;

    public MenuService(MenuItemRepository menuItemRepository,
                        CategoryRepository categoryRepository,
                        BroadcastService broadcastService,
                        @Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.broadcastService = broadcastService;
        this.campusZone = ZoneId.of(timeZone);
    }

    public List<MenuItemDto> getAllItems() {
        return menuItemRepository.findAll().stream().map(MenuItemDto::from).toList();
    }

    /**
     * The student-facing menu: shows both in-stock and sold-out items (so the
     * "Sold out" badge can render instead of the item just vanishing), but
     * DOES apply meal-time category gating -- a Breakfast item disappears
     * once its window has passed. Admin's view (getAllItems) applies neither
     * filter, since admins need to manage everything regardless of time.
     */
    public List<MenuItemDto> getStudentMenuItems() {
        return menuItemRepository.findAll().stream()
                .filter(item -> isCategoryCurrentlyAvailable(item.getCategory()))
                .map(MenuItemDto::from).toList();
    }

    public List<MenuItemDto> getAvailableItems() {
        return menuItemRepository.findByAvailableTrue().stream()
                .filter(item -> isCategoryCurrentlyAvailable(item.getCategory()))
                .map(MenuItemDto::from).toList();
    }

    public List<MenuItemDto> getDailySpecials() {
        return menuItemRepository.findByDailySpecialTrue().stream()
                .filter(item -> isCategoryCurrentlyAvailable(item.getCategory()))
                .map(MenuItemDto::from).toList();
    }

    /**
     * A category with no time window set is always visible. One with a
     * window (e.g. Breakfast 6:00-11:00) is only shown to students inside
     * that window -- handles overnight wraparound too (e.g. Dinner
     * 18:00-02:00 spans midnight).
     */
    private boolean isCategoryCurrentlyAvailable(Category category) {
        Integer from = category.getAvailableFromMinuteOfDay();
        Integer to = category.getAvailableToMinuteOfDay();
        if (from == null || to == null) {
            return true;
        }
        LocalTime now = LocalTime.now(campusZone);
        int nowMinute = now.getHour() * 60 + now.getMinute();
        if (from <= to) {
            return nowMinute >= from && nowMinute <= to;
        } else {
            return nowMinute >= from || nowMinute <= to; // wraps past midnight
        }
    }

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        Category category = new Category(request.name(), request.displayOrder());
        category.setAvailableFromMinuteOfDay(request.availableFromMinuteOfDay());
        category.setAvailableToMinuteOfDay(request.availableToMinuteOfDay());
        return CategoryDto.from(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto updateCategory(Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(request.name());
        category.setDisplayOrder(request.displayOrder());
        category.setAvailableFromMinuteOfDay(request.availableFromMinuteOfDay());
        category.setAvailableToMinuteOfDay(request.availableToMinuteOfDay());
        CategoryDto dto = CategoryDto.from(categoryRepository.save(category));
        broadcastService.broadcastMenuUpdate();
        return dto;
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found");
        }
        try {
            categoryRepository.deleteById(categoryId);
            broadcastService.broadcastMenuUpdate();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This category cannot be deleted while it still contains menu items or historical orders.");
        }
    }

    @Transactional
    public MenuItemDto createItem(MenuItemRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        MenuItem item = new MenuItem();
        applyRequest(item, request, category);
        MenuItem saved = menuItemRepository.save(item);
        broadcastService.broadcastMenuUpdate();
        return MenuItemDto.from(saved);
    }

    @Transactional
    public MenuItemDto updateItem(Long itemId, MenuItemRequest request) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        applyRequest(item, request, category);
        MenuItem saved = menuItemRepository.save(item);
        broadcastService.broadcastMenuUpdate();
        return MenuItemDto.from(saved);
    }

    @Transactional
    public MenuItemDto setAvailability(Long itemId, boolean available) {
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found"));
        item.setAvailable(available);
        MenuItem saved = menuItemRepository.save(item);
        // This is the "instantly reflects on the student menu" requirement --
        // push the change over the websocket so open menu screens update live.
        broadcastService.broadcastMenuUpdate();
        return MenuItemDto.from(saved);
    }

    @Transactional
    public void deleteItem(Long itemId) {
        if (!menuItemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Menu item not found");
        }
        try {
            menuItemRepository.deleteById(itemId);
            broadcastService.broadcastMenuUpdate();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("This menu item has historical references and cannot be deleted. Mark it sold out instead.");
        }
    }

    private void applyRequest(MenuItem item, MenuItemRequest request, Category category) {
        item.setName(request.name());
        item.setDescription(request.description());
        item.setPrice(request.price());
        item.setCategory(category);
        if (request.imageUrl() != null && !request.imageUrl().isBlank()
                && !request.imageUrl().trim().matches("https://[^\\s\"'<>]+")) {
            throw new com.campusbite.common.exception.BadRequestException("Menu image URL must use HTTPS");
        }
        item.setImageUrl(request.imageUrl() == null ? null : request.imageUrl().trim());
        item.setAvailable(request.available());
        item.setDailySpecial(request.dailySpecial());
        if (request.baselinePrepTimeMinutes() > 0) {
            item.setBaselinePrepTimeMinutes(request.baselinePrepTimeMinutes());
        }
        if (request.customizationOptions() != null) {
            item.setCustomizationOptions(request.customizationOptions());
        }
        if (request.dietType() != null) {
            item.setDietType(request.dietType());
        }
        if (request.allergens() != null) {
            item.setAllergens(request.allergens());
        }
    }
}
