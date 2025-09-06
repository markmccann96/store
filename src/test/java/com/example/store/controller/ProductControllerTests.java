package com.example.store.controller;

import com.example.store.api.dto.ProductCreateDTO;
import com.example.store.entity.Product;
import com.example.store.mapper.ProductMapper;
import com.example.store.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ComponentScan(basePackageClasses = ProductMapper.class)
class ProductControllerTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean
    private ProductRepository productRepository;

    private Product p1;
    private Product p2;

    @BeforeEach
    void setUp() {
        p1 = Product.builder().id(100L).description("Widget").build();
        p2 = Product.builder().id(200L).description("Gadget").build();
    }

    /* ---------- POST /products ---------- */

    @Test
    @SneakyThrows
    void createProduct_created201() {
        // exists? no. save returns the entity
        when(productRepository.existsById(100L)).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product toSave = inv.getArgument(0);
            return Product.builder().id(100L).description(toSave.getDescription()).build();
        });

        var body = new ProductCreateDTO();
        body.setId(100L);
        body.setDescription("Widget");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/products/100"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.description").value("Widget"))
                .andExpect(jsonPath("$.orders", hasSize(0)));
    }

    @Test
    @SneakyThrows
    void createProduct_conflict409_whenIdExists() {
        when(productRepository.existsById(100L)).thenReturn(true);

        var body = new ProductCreateDTO();
        body.setId(100L);
        body.setDescription("Widget");

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body)))
                .andExpect(status().isConflict());
    }

    @Test
    @SneakyThrows
    void createProduct_badRequest400_whenMissingDescription() {

        var body2 = new ProductCreateDTO();
        body2.setId(123L);
        body2.setDescription("    "); // blank

        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(body2)))
                .andExpect(status().isBadRequest());
    }

    /* ---------- GET /products/{id} ---------- */

    @Test
    @SneakyThrows
    void getProductById_ok200() {
        when(productRepository.findWithOrdersById(100L)).thenReturn(Optional.of(p1));
        when(productRepository.findOrderIdsByProductId(100L)).thenReturn(List.of(1L, 2L));

        mockMvc.perform(get("/products/100"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.description").value("Widget"))
                .andExpect(jsonPath("$.orders", hasSize(2)))
                .andExpect(jsonPath("$.orders[0]").value(1))
                .andExpect(jsonPath("$.orders[1]").value(2));
    }

    @Test
    @SneakyThrows
    void getProductById_notFound404() {
        when(productRepository.findWithOrdersById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/products/999"))
                .andExpect(status().isNotFound());
    }

    /* ---------- GET /products (no limit = all) ---------- */

    @Test
    @SneakyThrows
    void getProducts_all_ok200_withTotalHeader() {
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));
        when(productRepository.findOrderIdsByProductId(100L)).thenReturn(List.of(1L));
        when(productRepository.findOrderIdsByProductId(200L)).thenReturn(List.of(1L, 2L));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                // id = 100
                .andExpect(jsonPath("$[?(@.id==100)]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id==100)].description").value("Widget"))
                .andExpect(jsonPath("$[?(@.id==100)].orders", hasSize(1)))
                .andExpect(jsonPath("$[?(@.id==200)].description").value("Gadget"));
    }

    /* ---------- GET /products?limit=...&offset=... ---------- */

    @Test
    @SneakyThrows
    void getProducts_paged_ok200_withHeaders() {
        PageRequest pr = PageRequest.of(0, 1);
        Page<Product> page0 = new PageImpl<>(List.of(p1), pr, 2); // total=2
        when(productRepository.findAll(any(PageRequest.class))).thenReturn(page0);
        when(productRepository.findOrderIdsByProductId(100L)).thenReturn(List.of(42L));

        mockMvc.perform(get("/products?limit=1&offset=0"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                // Link header is generated by ResponseUtility; we don't assert its exact value here.
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].description").value("Widget"))
                .andExpect(jsonPath("$[0].orders", hasSize(1)))
                .andExpect(jsonPath("$[0].orders[0]").value(42));
    }
}
