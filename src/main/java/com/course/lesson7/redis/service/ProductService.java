package com.course.lesson7.redis.service;

import com.course.lesson7.redis.exception.ProductNotFoundException;
import com.course.lesson7.redis.model.entity.Category;
import com.course.lesson7.redis.model.entity.Product;
import com.course.lesson7.redis.model.enums.ProductStatus;
import com.course.lesson7.redis.model.request.CreateProductRequest;
import com.course.lesson7.redis.model.request.UpdateProductRequest;
import com.course.lesson7.redis.model.response.ProductResponse;
import com.course.lesson7.redis.repository.ProductRepository;
import com.course.lesson7.redis.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        log.info("Fetching product from database: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        return mapToResponse(product, false);
    }

    @Cacheable(value = "products", key = "'sku:' + #sku")
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.info("Fetching product from database by SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + sku));

        return mapToResponse(product, false);
    }

    @Transactional
    @CachePut(value = "products", key = "#result.id")
    @CacheEvict(value = "analytics", allEntries = true)
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating new product: {}", request.getSku());

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .status(request.getStatus())
                .imageUrl(request.getImageUrl())
                .viewCount(0)
                .build();

        product = productRepository.save(product);
        return mapToResponse(product, false);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "products", key = "#id"),
            evict = {
                    @CacheEvict(value = "products", key = "'sku:' + #result.sku"),
                    @CacheEvict(value = "analytics", allEntries = true)
            }
    )
    public ProductResponse updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getStockQuantity() != null) {
            product.setStockQuantity(request.getStockQuantity());
        }
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Category not found"));
            product.setCategory(category);
        }
        if (request.getStatus() != null) {
            product.setStatus(request.getStatus());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        product = productRepository.save(product);
        return mapToResponse(product, false);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        log.info("Deleting product: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(product -> mapToResponse(product, false));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(String keyword, Pageable pageable) {
        return productRepository.searchProducts(keyword, pageable)
                .map(product -> mapToResponse(product, false));
    }

    @Cacheable(value = "analytics", key = "'most-viewed'")
    @Transactional(readOnly = true)
    public List<ProductResponse> getMostViewedProducts(int limit) {
        log.info("Fetching most viewed products from database");

        return productRepository.findMostViewedProducts(Pageable.ofSize(limit))
                .stream()
                .map(product -> mapToResponse(product, false))
                .collect(Collectors.toList());
    }

    @Transactional
    @CacheEvict(value = {"products", "analytics"}, key = "#id")
    public void incrementViewCount(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + id));

        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);
    }

    @CacheEvict(value = "products", allEntries = true)
    public void clearCache() {
        log.info("Clearing all product caches");
    }

    private ProductResponse mapToResponse(Product product, boolean fromCache) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStockQuantity())
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .status(product.getStatus())
                .viewCount(product.getViewCount())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .fromCache(fromCache)
                .cacheTime(fromCache ? LocalDateTime.now() : null)
                .build();
    }
}
