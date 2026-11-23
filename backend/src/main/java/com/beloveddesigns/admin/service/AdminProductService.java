package com.beloveddesigns.admin.service;

import com.beloveddesigns.admin.dto.AdminProductResponse;
import com.beloveddesigns.admin.dto.CreateProductRequest;
import com.beloveddesigns.category.entity.Category;
import com.beloveddesigns.category.repository.CategoryRepository;
import com.beloveddesigns.except.BadRequestException;
import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.product.entity.Product;
import com.beloveddesigns.product.entity.ProductImage;
import com.beloveddesigns.product.entity.ProductVariant;
import com.beloveddesigns.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /** Liste admin : inclut les produits inactifs. Sans catégorie → tous les produits. */
    public Page<AdminProductResponse> list(List<String> categorySlugs, Pageable pageable) {
        Page<Product> page = (categorySlugs == null || categorySlugs.isEmpty())
                ? productRepository.findAll(pageable)
                : productRepository.findDistinctByCategories_SlugIn(categorySlugs, pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public AdminProductResponse create(CreateProductRequest request) {
        Set<Category> categories = resolveCategories(request.categorySlugs());
        String slug = resolveSlug(request.slug(), request.name());

        Product product = Product.builder()
                .name(request.name().trim())
                .slug(slug)
                .description(blankToNull(request.description()))
                .price(request.price())
                .stockQuantity(request.stockQuantity() != null ? request.stockQuantity() : 0)
                .isFeatured(Boolean.TRUE.equals(request.featured()))
                .isActive(request.active() == null || request.active())
                .categories(categories)
                .build();

        List<String> imageUrls = request.imageUrls() != null ? request.imageUrls() : List.of();
        for (int i = 0; i < imageUrls.size(); i++) {
            product.getImages().add(ProductImage.builder()
                    .product(product)
                    .url(imageUrls.get(i).trim())
                    .position(i)
                    .build());
        }

        if (request.variants() != null) {
            for (CreateProductRequest.VariantRequest v : request.variants()) {
                product.getVariants().add(ProductVariant.builder()
                        .product(product)
                        .size(blankToNull(v.size()))
                        .color(blankToNull(v.color()))
                        .stockQuantity(v.stockQuantity() != null ? v.stockQuantity() : 0)
                        .build());
            }
        }

        return toResponse(productRepository.save(product));
    }

    private Set<Category> resolveCategories(List<String> slugs) {
        Set<String> wanted = new LinkedHashSet<>(slugs);
        Set<Category> found = new HashSet<>();
        List<String> missing = new ArrayList<>();
        for (String slug : wanted) {
            categoryRepository.findBySlug(slug).ifPresentOrElse(found::add, () -> missing.add(slug));
        }
        if (!missing.isEmpty()) {
            throw new BadRequestException(ErrorMessages.categoriesNotFound(missing));
        }
        return found;
    }

    /** Slug fourni → doit être libre. Slug absent → généré depuis le nom, suffixé (-2, -3...) si déjà pris. */
    private String resolveSlug(String requested, String name) {
        if (requested != null && !requested.isBlank()) {
            if (productRepository.existsBySlug(requested)) {
                throw new BadRequestException(ErrorMessages.productSlugAlreadyUsed(requested));
            }
            return requested;
        }
        String base = slugify(name);
        String candidate = base;
        int suffix = 2;
        while (productRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    static String slugify(String value) {
        String ascii = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return slug.isEmpty() ? "produit" : slug;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    AdminProductResponse toResponse(Product product) {
        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getCurrency(),
                product.getStockQuantity(),
                product.isFeatured(),
                product.isActive(),
                product.getImages().stream()
                        .sorted(Comparator.comparing(ProductImage::getPosition))
                        .map(ProductImage::getUrl)
                        .toList(),
                product.getCategories().stream().map(Category::getSlug).sorted().toList(),
                product.getVariants().stream()
                        .map(v -> new AdminProductResponse.VariantResponse(
                                v.getId(), v.getSize(), v.getColor(), v.getStockQuantity()))
                        .toList(),
                product.getCreatedAt()
        );
    }
}
