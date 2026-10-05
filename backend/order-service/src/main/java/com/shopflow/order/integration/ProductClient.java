package com.shopflow.order.integration;

import com.shopflow.order.integration.dto.ProductSnapshot;
import com.shopflow.order.integration.dto.StockOperation;
import com.shopflow.order.integration.dto.StockView;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "product-service", path = "/api/v1/internal/products")
public interface ProductClient {

    @GetMapping("/{id}")
    ProductSnapshot findById(@PathVariable("id") Long id);

    @PostMapping("/{id}/stock/reserve")
    StockView reserve(@PathVariable("id") Long id, @RequestBody StockOperation body);

    @PostMapping("/{id}/stock/release")
    StockView release(@PathVariable("id") Long id, @RequestBody StockOperation body);
}
