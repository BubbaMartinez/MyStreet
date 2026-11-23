package com.mystreet.config;

import com.mystreet.domain.Product;
import com.mystreet.domain.User;
import com.mystreet.repository.ProductRepository;
import com.mystreet.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Seed idempotente: admin inicial y 8 sneakers. Cambiar la contraseña del admin fuera de dev. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final ProductRepository products;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, ProductRepository products, PasswordEncoder encoder) {
        this.users = users;
        this.products = products;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (!users.existsByEmail("admin@mystreet.com")) {
            User admin = new User();
            admin.setEmail("admin@mystreet.com");
            admin.setFullName("MyStreeT Admin");
            admin.setPasswordHash(encoder.encode("Admin123!"));
            admin.setAdmin(true);
            users.save(admin);
        }
        if (products.count() == 0) {
            products.save(sneaker("Air Zoom Pegasus", "Nike", "129.99", 25, "8", "9", "10", "11"));
            products.save(sneaker("Air Max 90", "Nike", "139.99", 18, "8", "9", "10"));
            products.save(sneaker("Ultraboost Light", "Adidas", "179.99", 12, "9", "10", "11", "12"));
            products.save(sneaker("Samba OG", "Adidas", "109.99", 30, "7", "8", "9", "10"));
            products.save(sneaker("574 Core", "New Balance", "99.99", 20, "8", "9", "10", "11"));
            products.save(sneaker("990v6", "New Balance", "199.99", 8, "9", "10", "11"));
            products.save(sneaker("Classic Leather", "Reebok", "89.99", 22, "7", "8", "9", "10", "11"));
            products.save(sneaker("Chuck 70 High", "Converse", "94.99", 15, "6", "7", "8", "9"));
        }
    }

    private Product sneaker(String name, String brand, String price, int stock, String... sizes) {
        Product p = new Product();
        p.setName(name);
        p.setBrand(brand);
        p.setDescription(name + " by " + brand);
        p.setPrice(new BigDecimal(price));
        p.setImageUrl("https://placehold.co/600x400?text=" + name.replace(' ', '+'));
        p.setSizes(Set.of(sizes));
        p.setStockQty(stock);
        return p;
    }
}
