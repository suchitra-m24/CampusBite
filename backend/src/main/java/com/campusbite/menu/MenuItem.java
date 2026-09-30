package com.campusbite.menu;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_items")
public class MenuItem extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column
    private String imageUrl;

    @Column(nullable = false)
    private boolean available = true;

    @Column(nullable = false)
    private boolean dailySpecial = false;

    /** Average minutes the kitchen has historically taken to prepare this item; seeds slot allocation. */
    @Column(nullable = false)
    private int baselinePrepTimeMinutes = 8;

    @ElementCollection
    @CollectionTable(name = "menu_item_customizations", joinColumns = @JoinColumn(name = "menu_item_id"))
    @Column(name = "option_label")
    private List<String> customizationOptions = new ArrayList<>();

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @Column(nullable = false)
    private DietType dietType = DietType.VEG;

    @ElementCollection
    @CollectionTable(name = "menu_item_allergens", joinColumns = @JoinColumn(name = "menu_item_id"))
    @Column(name = "allergen")
    private List<String> allergens = new ArrayList<>();

    public MenuItem() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isDailySpecial() {
        return dailySpecial;
    }

    public void setDailySpecial(boolean dailySpecial) {
        this.dailySpecial = dailySpecial;
    }

    public int getBaselinePrepTimeMinutes() {
        return baselinePrepTimeMinutes;
    }

    public void setBaselinePrepTimeMinutes(int baselinePrepTimeMinutes) {
        this.baselinePrepTimeMinutes = baselinePrepTimeMinutes;
    }

    public List<String> getCustomizationOptions() {
        return customizationOptions;
    }

    public void setCustomizationOptions(List<String> customizationOptions) {
        this.customizationOptions = customizationOptions;
    }

    public DietType getDietType() {
        return dietType;
    }

    public void setDietType(DietType dietType) {
        this.dietType = dietType;
    }

    public List<String> getAllergens() {
        return allergens;
    }

    public void setAllergens(List<String> allergens) {
        this.allergens = allergens;
    }
}
