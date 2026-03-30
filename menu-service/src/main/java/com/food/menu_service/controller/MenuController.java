package com.food.menu_service.controller;

import com.food.menu_service.model.MenuItem;
import com.food.menu_service.repository.MenuRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
@RequiredArgsConstructor
@Tag(name = "Menu Controller", description = "Endpoints for managing menu items")
public class MenuController {

    private final MenuRepository menuRepository;

    @GetMapping
    @Operation(summary = "Get all menu items")
    public List<MenuItem> getAllMenuItems() {
        return menuRepository.findAll();
    }

    @PostMapping
    @Operation(summary = "Add a new menu item")
    public MenuItem addMenuItem(@RequestBody MenuItem menuItem) {
        return menuRepository.save(menuItem);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a menu item by ID")
    public ResponseEntity<MenuItem> getMenuItemById(@PathVariable Long id) {
        return menuRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing menu item")
    public ResponseEntity<MenuItem> updateMenuItem(@PathVariable Long id, @RequestBody MenuItem updatedItem) {
        return menuRepository.findById(id)
                .map(item -> {
                    item.setName(updatedItem.getName());
                    item.setDescription(updatedItem.getDescription());
                    item.setPrice(updatedItem.getPrice());
                    item.setCategory(updatedItem.getCategory());
                    item.setAvailable(updatedItem.isAvailable());
                    return ResponseEntity.ok(menuRepository.save(item));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a menu item")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        if (menuRepository.existsById(id)) {
            menuRepository.deleteById(id);
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
