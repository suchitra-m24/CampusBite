package com.campusbite.common.seed;

import com.campusbite.menu.Category;
import com.campusbite.menu.CategoryRepository;
import com.campusbite.menu.MenuItem;
import com.campusbite.menu.MenuItemRepository;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds the starter menu on first boot. Privileged demo accounts are only seeded
 * when explicitly enabled through environment variables, so default deployments
 * do not create published credentials.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean demoAccountsEnabled;
    private final String seedAdminPassword;
    private final String seedStaffPassword;

    public DataSeeder(UserRepository userRepository, CategoryRepository categoryRepository,
                       MenuItemRepository menuItemRepository, PasswordEncoder passwordEncoder,
                       @Value("${campusbite.seed.demo-accounts-enabled:false}") boolean demoAccountsEnabled,
                       @Value("${SEED_ADMIN_PASSWORD:}") String seedAdminPassword,
                       @Value("${SEED_STAFF_PASSWORD:}") String seedStaffPassword) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoAccountsEnabled = demoAccountsEnabled;
        this.seedAdminPassword = seedAdminPassword;
        this.seedStaffPassword = seedStaffPassword;
    }

    @Override
    public void run(String... args) {
        if (demoAccountsEnabled) {
            if (seedAdminPassword == null || seedAdminPassword.isBlank() || seedStaffPassword == null || seedStaffPassword.isBlank()) {
                throw new IllegalStateException("Demo accounts are enabled but SEED_ADMIN_PASSWORD and SEED_STAFF_PASSWORD are not configured");
            }
            if (!userRepository.existsByEmail("admin@campusbite.dev")) {
                User admin = new User("Campus Admin", "admin@campusbite.dev", "ADMIN001",
                        passwordEncoder.encode(seedAdminPassword), Role.ADMIN);
                admin.setEmailVerified(true);
                userRepository.save(admin);
            }
            if (!userRepository.existsByEmail("staff@campusbite.dev")) {
                User staff = new User("Kitchen Staff", "staff@campusbite.dev", "STAFF001",
                        passwordEncoder.encode(seedStaffPassword), Role.STAFF);
                staff.setEmailVerified(true);
                userRepository.save(staff);
            }
        }

        if (categoryRepository.count() == 0) {
            Category mains = categoryRepository.save(new Category("Mains", 1));
            Category snacks = categoryRepository.save(new Category("Snacks", 2));
            Category beverages = categoryRepository.save(new Category("Beverages", 3));
            Category desserts = categoryRepository.save(new Category("Desserts", 4));

            seedItem("Paneer Butter Masala Bowl", "Creamy tomato gravy, paneer, served with jeera rice", "349.00", mains, 12, true);
            seedItem("Veg Thali", "Dal, sabzi, rice, 3 rotis, salad", "199.00", mains, 15, false);
            seedItem("Chicken Biryani", "Hyderabadi-style, served with raita", "259.00", mains, 18, true);
            seedItem("Masala Dosa", "Crisp dosa, potato masala, chutney & sambar", "129.00", mains, 10, false);
            seedItem("Veg Sandwich", "Grilled, with mint chutney", "89.00", snacks, 6, false);
            seedItem("Samosa (2 pcs)", "Classic potato samosa with tamarind chutney", "49.00", snacks, 5, false);
            seedItem("French Fries", "Crispy, peri-peri seasoning", "79.00", snacks, 7, false);
            seedItem("Cold Coffee", "Blended with ice cream", "99.00", beverages, 4, false);
            seedItem("Masala Chai", "Freshly brewed", "29.00", beverages, 3, false);
            seedItem("Fresh Lime Soda", "Sweet, salted or plain", "49.00", beverages, 3, false);
            seedItem("Gulab Jamun (2 pcs)", "Warm, syrup-soaked", "59.00", desserts, 4, false);
            seedItem("Chocolate Brownie", "Served warm with a scoop of vanilla", "89.00", desserts, 6, true);
        }
    }

    private void seedItem(String name, String desc, String price, Category category, int prepMinutes, boolean special) {
        MenuItem item = new MenuItem();
        item.setName(name);
        item.setDescription(desc);
        item.setPrice(new BigDecimal(price));
        item.setCategory(category);
        item.setAvailable(true);
        item.setDailySpecial(special);
        item.setBaselinePrepTimeMinutes(prepMinutes);
        item.setCustomizationOptions(List.of("Regular", "Extra spicy", "Less spicy"));
        menuItemRepository.save(item);
    }
}
