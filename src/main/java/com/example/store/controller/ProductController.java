package com.example.store.controller;

import com.example.store.api.dto.ProductCreateDTO;
import com.example.store.api.dto.ProductDTO;
import com.example.store.controller.api.ProductApi;
import com.example.store.entity.Product;
import com.example.store.repository.ProductRepository;
import com.example.store.mapper.ProductMapper;

import com.example.store.utils.ResponseUtility;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class ProductController implements ProductApi {

    private final ProductRepository productRepository;
    private final ProductMapper mapper;

    @Override
    public ResponseEntity<ProductDTO> createProduct(@Valid ProductCreateDTO body) {
        if ( body.getDescription() == null || body.getDescription().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (productRepository.existsById(body.getId())) {
            return ResponseEntity.status(409).build();
        }

        Product toSave = mapper.toEntity(body);
        // id is created on save.
        toSave.setId(null);
        try {
            Product saved = productRepository.save(toSave);
            // Newly created product has no orders yet → orders list will be empty.
            ProductDTO dto = mapper.toDto(saved, List.of());

            URI location = ServletUriComponentsBuilder
                    .fromCurrentRequestUri()
                    .path("/{id}")
                    .buildAndExpand(saved.getId())
                    .toUri();

            return ResponseEntity.created(location)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto);
        } catch (DataIntegrityViolationException ex) {
            // e.g., constraint violation (duplicate key)
            return ResponseEntity.status(409).build();
        }
    }

    @Override
    public ResponseEntity<ProductDTO> getProductById(Long id) {
        return productRepository.findWithOrdersById(id)
                .map(p -> {
                    // Ensure we have order IDs even if orders weren’t fetched eagerly
                    List<Long> orderIds = productRepository.findOrderIdsByProductId(p.getId());
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(mapper.toDto(p, orderIds));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<ProductDTO>> getProducts(Integer limit, Integer offset) {
        // Backward compatibility: if limit is omitted → return ALL (but still set X-Total-Count)
        if (limit == null) {
            List<Product> all = productRepository.findAll(); // annotated with @EntityGraph in repo
            List<ProductDTO> payload = all.stream()
                    .map(p -> mapper.toDto(p, productRepository.findOrderIdsByProductId(p.getId())))
                    .toList();

            long total = all.size();
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-Total-Count", String.valueOf(total));
            // No Link header for non-paginated response
            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload);
        }

        int safeOffset = Math.max(0, Optional.ofNullable(offset).orElse(0));
        int safeLimit = Math.max(1, limit);

        // Spring Data is page/size; our API is offset/limit.
        int page = safeOffset / safeLimit;
        PageRequest pr = PageRequest.of(page, safeLimit);

        Page<Product> pageData = productRepository.findAll(pr);
        List<ProductDTO> items = pageData.getContent().stream()
                .map(p -> mapper.toDto(p, productRepository.findOrderIdsByProductId(p.getId())))
                .toList();

        long total = pageData.getTotalElements();

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(total));
        headers.add(HttpHeaders.LINK, ResponseUtility.buildLinkHeader(total, safeLimit, safeOffset));

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(items);
    }

}
