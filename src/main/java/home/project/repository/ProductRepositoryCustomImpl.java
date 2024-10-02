package home.project.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import home.project.domain.Category;
import home.project.domain.Product;
import home.project.domain.QCategory;
import home.project.domain.QProduct;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;

import java.util.List;

public class ProductRepositoryCustomImpl implements ProductRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QProduct product = QProduct.product;
    private final QCategory category = QCategory.category;

    public ProductRepositoryCustomImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public Page<Product> findProducts(String brand, String categoryCode, String productName, String content, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (brand != null && !brand.isEmpty()) {
            builder.and(product.brand.toLowerCase().like("%" + brand.toLowerCase() + "%"));
        }
        if (categoryCode != null && !categoryCode.isEmpty()) {
            builder.and(product.category.code.toLowerCase().like("%" + categoryCode.toLowerCase() + "%"));
        }
        if (productName != null && !productName.isEmpty()) {
            builder.and(product.name.toLowerCase().like("%" + productName.toLowerCase() + "%"));
        }
        if (content != null && !content.isEmpty()) {
            builder.and(product.brand.toLowerCase()
                    .concat(" ").concat(product.category.code.toLowerCase())
                    .concat(" ").concat(product.name.toLowerCase())
                    .like("%" + content.toLowerCase() + "%"));
        }

        List<Product> results = queryFactory
                .selectFrom(product)
                .leftJoin(product.category, category).fetchJoin()
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(product)
                .where(builder)
                .fetchCount();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public Page<Product> findAllByOrderByBrandAsc(Pageable pageable) {
        List<Product> results = queryFactory
                .selectFrom(product)
                .orderBy(product.brand.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = queryFactory
                .selectFrom(product)
                .fetchCount();

        return new PageImpl<>(results, pageable, total);
    }

    @Override
    public List<Product> findAllByCategory(Category category) {
        return queryFactory
                .selectFrom(product)
                .where(product.category.id.eq(category.getId()))
                .fetch();
    }

}