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
            new DefaultCategoryDef("Ovqat", "🍽"),
            new DefaultCategoryDef("Transport", "🚕"),
            new DefaultCategoryDef("Uy", "🏠"),
            new DefaultCategoryDef("Bozor", "🛒"),
            new DefaultCategoryDef("Yoqilg‘i", "⛽"),
            new DefaultCategoryDef("Ish", "🔧"),
            new DefaultCategoryDef("Material", "📦"),
            new DefaultCategoryDef("Kommunal", "💡"),
            new DefaultCategoryDef("Aloqa / Internet", "📱"),
            new DefaultCategoryDef("Oila", "👨‍👩‍👧"),
            new DefaultCategoryDef("Sovg‘a", "🎁"),
            new DefaultCategoryDef("Dori", "💊"),
            new DefaultCategoryDef("Ko‘ngilochar", "🎉"),
            new DefaultCategoryDef("Kredit / Qarzdorlik", "💳"),
            new DefaultCategoryDef("Ta’lim", "📚"),
            new DefaultCategoryDef("Sayohat", "✈️"),
            new DefaultCategoryDef("Boshqa", "📌")
    );

    private static final List<DefaultCategoryDef> DEFAULT_INCOMES = List.of(
            new DefaultCategoryDef("Ish", "💼"),
            new DefaultCategoryDef("Xizmat", "🛠"),
            new DefaultCategoryDef("Savdo", "🛍"),
            new DefaultCategoryDef("Naqd", "💵"),
            new DefaultCategoryDef("O‘tkazma", "🏦"),
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
        return categoryRepository.findByUserIdAndNameIgnoreCaseAndType(userId, name, type);
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
}
