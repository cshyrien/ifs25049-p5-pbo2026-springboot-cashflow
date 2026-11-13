package org.delcom.ifs25049springbootcashflow.adapter.repository;

import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRepositoryTest {

    @Test
    void saveGeneratesIncrementingIds() {
        TransactionRepository repository = new TransactionRepository();
        Transaction first = repository.save(1, "Gaji", 1000L, TransactionType.INCOME);
        Transaction second = repository.save(1, "Makan", 500L, TransactionType.EXPENSE);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void findByUserIdFiltersOwner() {
        TransactionRepository repository = new TransactionRepository();
        repository.save(1, "a", 100L, TransactionType.INCOME);
        repository.save(2, "b", 200L, TransactionType.INCOME);
        repository.save(1, "c", 300L, TransactionType.EXPENSE);

        assertEquals(2, repository.findByUserId(1).size());
        assertEquals(1, repository.findByUserId(2).size());
        assertTrue(repository.findByUserId(99).isEmpty());
    }

    @Test
    void findByIdAndDelete() {
        TransactionRepository repository = new TransactionRepository();
        Transaction saved = repository.save(1, "a", 100L, TransactionType.INCOME);

        assertTrue(repository.findById(saved.getId()).isPresent());
        assertTrue(repository.findById(99).isEmpty());
        assertTrue(repository.deleteById(saved.getId()));
        assertFalse(repository.deleteById(saved.getId()));
    }

    @Test
    void updateKeepsMutatedEntity() {
        TransactionRepository repository = new TransactionRepository();
        Transaction saved = repository.save(1, "a", 100L, TransactionType.INCOME);
        saved.changeAmount(750L);
        repository.update(saved);

        assertEquals(750L, repository.findById(saved.getId()).orElseThrow().getAmount());
        assertEquals(1, repository.findBy(t -> t.getAmount() == 750L).size());
    }
}
