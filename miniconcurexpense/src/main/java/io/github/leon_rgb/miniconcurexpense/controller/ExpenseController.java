package io.github.leon_rgb.miniconcurexpense.controller;

import io.github.leon_rgb.miniconcurexpense.model.Expense;
import io.github.leon_rgb.miniconcurexpense.repository.ExpenseRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public List<Expense> getAll() {
        return repository.findAll();
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
