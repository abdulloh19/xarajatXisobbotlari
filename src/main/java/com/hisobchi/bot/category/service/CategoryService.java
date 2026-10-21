package com.hisobchi.bot.category.service;

import com.hisobchi.bot.category.entity.Category;
import com.hisobchi.bot.category.entity.CategoryType;
import com.hisobchi.bot.category.repository.CategoryRepository;
import com.hisobchi.bot.common.exception.EntityNotFoundException;
import com.hisobchi.bot.common.exception.ValidationException;
import com.hisobchi.bot.transaction.entity.TransactionType;
import com.hisobchi.bot.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    private static final List<DefaultCategoryDef> DEFAULT_EXPENSES = List.of(
            new DefaultCategoryDef("Oziq-ovqat", "🍽"),
            new DefaultCategoryDef("Transport va Yoqilg‘i", "🚕"),
            new DefaultCategoryDef("Ro‘zg‘or va Kommunal", "🏠"),
            new DefaultCategoryDef("Oila va Salomatlik", "👨‍👩‍👧"),
            new DefaultCategoryDef("Biznes va Ish", "💼"),
            new DefaultCategoryDef("Boshqa", "📌")
    );

    private static final List<DefaultCategoryDef> DEFAULT_INCOMES = List.of(
            new DefaultCategoryDef("Savdo va Foyda", "💰"),
            new DefaultCategoryDef("Oylik va Ish haqi", "💼"),
            new DefaultCategoryDef("Xizmat ko‘rsatish", "🛠"),
            new DefaultCategoryDef("Boshqa", "📌")
    );

    private record DefaultCategoryDef(String name, String emoji) {}

    @Transactional
    public void initDefaultCategoriesIfNone(User user) {
        if (categoryRepository.countByUserId(user.getId()) > 0) {
            return;
        }
        log.info("Initializing default categories for user id: {}", user.getId());
        List<Category> categories = new ArrayList<>();

        for (DefaultCategoryDef def : DEFAULT_EXPENSES) {
            categories.add(Category.builder()
                    .user(user)
                    .name(def.name())
                    .emoji(def.emoji())
                    .type(CategoryType.EXPENSE)
                    .isDefault(true)
                    .isActive(true)
                    .build());
        }

        for (DefaultCategoryDef def : DEFAULT_INCOMES) {
            categories.add(Category.builder()
                    .user(user)
                    .name(def.name())
                    .emoji(def.emoji())
                    .type(CategoryType.INCOME)
                    .isDefault(true)
                    .isActive(true)
                    .build());
        }

        categoryRepository.saveAll(categories);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(Long userId, CategoryType type) {
        return categoryRepository.findByUserIdAndTypeAndIsActiveTrueOrderByIdAsc(userId, type);
    }

    @Transactional(readOnly = true)
    public List<Category> getCategories(Long userId, TransactionType type) {
        return getCategories(userId, CategoryType.from(type));
    }

    @Transactional(readOnly = true)
    public Category getById(Long id, Long userId) {
        return categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Kategoriya topilmadi yoki sizga tegishli emas: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Category> findByName(Long userId, String name, CategoryType type) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        Optional<Category> found = categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, name.trim(), type);
        if (found.isPresent()) {
            return found;
        }

        // Alias mapping to generalized categories
        String mapped = mapToGeneralizedCategoryName(name.trim(), type);
        if (mapped != null && !mapped.equalsIgnoreCase(name.trim())) {
            return categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, mapped, type);
        }

        return Optional.empty();
    }

    private String mapToGeneralizedCategoryName(String name, CategoryType type) {
        String lower = name.toLowerCase().replace("’", "'").replace("‘", "'").replace("`", "'");

        if (type == CategoryType.EXPENSE) {
            if (lower.contains("ovqat") || lower.contains("obed") || lower.contains("tushlik") || lower.contains("bozor") || lower.contains("oziq")) {
                return "Oziq-ovqat";
            }
            if (lower.contains("transport") || lower.contains("yoqilg") || lower.contains("benzin") || lower.contains("gaz") || lower.contains("taxi") || lower.contains("taksi")) {
                return "Transport va Yoqilg‘i";
            }
            if (lower.contains("ro'zg'or") || lower.contains("rozgor") || lower.contains("uy") || lower.contains("ijara") || lower.contains("arenda") || lower.contains("kommunal") || lower.contains("svet") || lower.contains("suv") || lower.contains("aloqa") || lower.contains("internet") || lower.contains("wifi")) {
                return "Ro‘zg‘or va Kommunal";
            }
            if (lower.contains("oila") || lower.contains("dori") || lower.contains("apteka") || lower.contains("salomatlik") || lower.contains("sovg'a") || lower.contains("sovga") || lower.contains("ko'ngil") || lower.contains("dam") || lower.contains("ta'lim") || lower.contains("talim") || lower.contains("bolalar") || lower.contains("sayohat")) {
                return "Oila va Salomatlik";
            }
            if (lower.contains("material") || lower.contains("ish") || lower.contains("biznes") || lower.contains("usta") || lower.contains("zapchast") || lower.contains("kredit") || lower.contains("soliq")) {
                return "Biznes va Ish";
            }
            return "Boshqa";
        } else if (type == CategoryType.INCOME) {
            if (lower.contains("savdo") || lower.contains("foyda") || lower.contains("daromad") || lower.contains("tushum") || lower.contains("sotuv") || lower.contains("magazin")) {
                return "Savdo va Foyda";
            }
            if (lower.contains("oylik") || lower.contains("maosh") || lower.contains("avans") || lower.contains("ish haqi") || lower.contains("ishhaqi") || lower.equals("ish")) {
                return "Oylik va Ish haqi";
            }
            if (lower.contains("xizmat") || lower.contains("montaj") || lower.contains("remont") || lower.contains("usta") || lower.contains("klient")) {
                return "Xizmat ko‘rsatish";
            }
            return "Boshqa";
        }
        return null;
    }

    @Transactional(readOnly = true)
    public Optional<Category> findByName(Long userId, String name, TransactionType type) {
        return findByName(userId, name, CategoryType.from(type));
    }

    @Transactional
    public Category createCustomCategory(User user, String name, String emoji, CategoryType type) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Kategoriya nomi bo‘sh bo‘lishi mumkin emas");
        }
        String cleanName = name.trim();
        String cleanEmoji = (emoji == null || emoji.isBlank()) ? "📌" : emoji.trim();

        if (categoryRepository.existsByUserIdAndNameIgnoreCaseAndType(user.getId(), cleanName, type)) {
            throw new ValidationException("Bu nomdagi kategoriya allaqachon mavjud: " + cleanName);
        }

        Category category = Category.builder()
                .user(user)
                .name(cleanName)
                .emoji(cleanEmoji)
                .type(type)
                .isDefault(false)
                .isActive(true)
                .build();

        return categoryRepository.save(category);
    }

    @Transactional
    public Category createCustomCategory(User user, String name, String emoji, TransactionType type) {
        return createCustomCategory(user, name, emoji, CategoryType.from(type));
    }

    @Transactional
    public Category getOrCreateCategory(User user, String name, String emoji, TransactionType type) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Kategoriya nomi bo‘sh bo‘lishi mumkin emas");
        }
        String cleanName = name.trim();
        CategoryType catType = CategoryType.from(type);
        return categoryRepository.findByUserIdAndNameIgnoreCaseAndType(user.getId(), cleanName, catType)
                .orElseGet(() -> createCustomCategory(user, cleanName, emoji, catType));
    }
}
