package io.github.leon_rgb.miniconcurexpense.controller;

import io.github.leon_rgb.miniconcurexpense.model.Expense;
import io.github.leon_rgb.miniconcurexpense.repository.ExpenseRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Controller for managing expenses.
 * Provides endpoints to create, retrieve, and delete expenses.
 */
@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseRepository repository;

    public ExpenseController(ExpenseRepository repository) {
        this.repository = repository;
    }

    /**
     * Return a page of expenses. Sort by id desc so the most recent (highest id)
     * appear first. Query params:
     *   ?page=0 (default)
     *   &size=20 (default)
     */
    @GetMapping
    public Page<Expense> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return repository.findAll(PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @PostMapping
    public Expense create(@RequestBody Expense expense) {
        return repository.save(expense);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repository.deleteById(id);
    }
    
    @DeleteMapping()
    public void delete() {
        repository.deleteAll();
    }
}
