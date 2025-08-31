package io.github.leon_rgb.miniconcurexpense.repository;

import io.github.leon_rgb.miniconcurexpense.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Expense entities. 
 * A repository in simple terms is a mechanism for encapsulating storage, retrieval, and search behavior which emulates a collection of objects.
 * By extending JpaRepository, this interface inherits several methods for working with Expense persistence, including methods for saving, deleting, and finding Expense entities.
 */
@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {
}
