package com.quizbattle.quiz.config;

import com.quizbattle.quiz.entity.*;
import com.quizbattle.quiz.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds reference data (roles, categories, achievements) and a bootstrap admin
 * account on first run. Idempotent — safe to run against a populated database.
 * Disabled in the {@code test} profile.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final RoleRepository roleRepository;
    private final CategoryRepository categoryRepository;
    private final AchievementRepository achievementRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminEmail;
    private final String adminPassword;

    public DataSeeder(RoleRepository roleRepository,
                      CategoryRepository categoryRepository,
                      AchievementRepository achievementRepository,
                      UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      @Value("${seed.admin.username:admin}") String adminUsername,
                      @Value("${seed.admin.email:admin@quizbattle.io}") String adminEmail,
                      @Value("${seed.admin.password:Admin12345!}") String adminPassword) {
        this.roleRepository = roleRepository;
        this.categoryRepository = categoryRepository;
        this.achievementRepository = achievementRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedCategories();
        seedAchievements();
        seedAdmin();
    }

    private void seedRoles() {
        for (RoleName name : RoleName.values()) {
            roleRepository.findByName(name)
                    .orElseGet(() -> roleRepository.save(Role.builder().name(name).build()));
        }
    }

    private void seedCategories() {
        record Cat(String name, String desc, String icon) {
        }
        List<Cat> defaults = List.of(
                new Cat("Programming", "Languages, algorithms and tools", "code"),
                new Cat("Mathematics", "Algebra, geometry and logic", "calculate"),
                new Cat("Science", "Physics, chemistry and biology", "science"),
                new Cat("History", "World events and civilizations", "history_edu"),
                new Cat("Geography", "Countries, capitals and maps", "public"),
                new Cat("Sports", "Games, athletes and records", "sports_soccer"),
                new Cat("General Knowledge", "A bit of everything", "lightbulb")
        );
        for (Cat c : defaults) {
            if (!categoryRepository.existsByName(c.name())) {
                categoryRepository.save(Category.builder()
                        .name(c.name()).description(c.desc()).icon(c.icon()).build());
            }
        }
    }

    private void seedAchievements() {
        record Ach(String code, String title, String desc, String icon, int xp) {
        }
        List<Ach> defaults = List.of(
                new Ach("FIRST_VICTORY", "First Victory", "Win your first battle", "emoji_events", 100),
                new Ach("TEN_WINS", "10 Wins", "Win 10 battles", "military_tech", 500),
                new Ach("QUIZ_MASTER", "Quiz Master", "Play 50 games", "workspace_premium", 1000),
                new Ach("PROGRAMMING_EXPERT", "Programming Expert", "Master the Programming category", "code", 750),
                new Ach("SCIENCE_CHAMPION", "Science Champion", "Master the Science category", "science", 750)
        );
        for (Ach a : defaults) {
            if (achievementRepository.findByCode(a.code()).isEmpty()) {
                achievementRepository.save(Achievement.builder()
                        .code(a.code()).title(a.title()).description(a.desc())
                        .icon(a.icon()).xpReward(a.xp()).build());
            }
        }
    }

    private void seedAdmin() {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }
        Role adminRole = roleRepository.findByName(RoleName.ADMINISTRATOR).orElseThrow();
        User admin = User.builder()
                .username(adminUsername)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .displayName("Administrator")
                .build();
        admin.addRole(adminRole);
        userRepository.save(admin);
        log.warn("Seeded bootstrap admin '{}' — change the password immediately in production!", adminUsername);
    }
}
