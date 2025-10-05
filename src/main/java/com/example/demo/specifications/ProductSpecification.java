package com.example.demo.specifications;
import org.springframework.data.jpa.domain.Specification;
import com.example.demo.model.Product;

public class ProductSpecification {
    private static Specification<Product> titleLike(String title) {
        return (root, query, criteriaBuilder) -> {
            if (title == null || title.trim().isEmpty()) {
                return null;
        }
            return criteriaBuilder.like(
                criteriaBuilder.lower(root.get("title")), "%" + title.toLowerCase().trim() + "%");
        };
    }
    private static Specification<Product> priceBetween(Integer min, Integer max) {
        return (root, query, criteriaBuilder) -> {
            if (min == null && max == null) {
                return null;
            } else if (min != null && max != null) {
                return criteriaBuilder.between(root.get("cost"), min, max);
            } else if (min != null) {
                return criteriaBuilder.greaterThanOrEqualTo(root.get("cost"), min);
            } else {
                return criteriaBuilder.lessThanOrEqualTo(root.get("cost"), max);
            }
        };
    }
    public static Specification<Product> filter(String title, Integer min, Integer max) {
        return Specification.allOf(titleLike(title)).and(priceBetween(min, max));
    }
}