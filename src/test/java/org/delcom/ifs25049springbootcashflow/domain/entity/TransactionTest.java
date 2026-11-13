package org.delcom.ifs25049springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    @Test
    void shortConstructorSetsDefaults() {
        Transaction transaction = new Transaction(1, 7, "Gaji", 5000000L, TransactionType.INCOME);

        assertEquals(1, transaction.getId());
        assertEquals(7, transaction.getUserId());
        assertEquals("Gaji", transaction.getDescription());
        assertEquals(5000000L, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertNull(transaction.getProofPath());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void fullConstructorKeepsAllFields() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 13, 10, 30);
        Transaction transaction = new Transaction(2, 3, "Makan", 25000L, TransactionType.EXPENSE, "proofs/a.png", now);

        assertEquals("proofs/a.png", transaction.getProofPath());
        assertEquals(now, transaction.getCreatedAt());
    }

    @Test
    void nullDescriptionBecomesEmpty() {
        Transaction transaction = new Transaction(1, 1, null, 100L, TransactionType.INCOME);
        assertEquals("", transaction.getDescription());
        transaction.changeDescription(null);
        assertEquals("", transaction.getDescription());
    }

    @Test
    void typeFlags() {
        Transaction income = new Transaction(1, 1, "a", 100L, TransactionType.INCOME);
        Transaction expense = new Transaction(2, 1, "b", 100L, TransactionType.EXPENSE);

        assertTrue(income.isIncome());
        assertFalse(income.isExpense());
        assertTrue(expense.isExpense());
        assertFalse(expense.isIncome());
    }

    @Test
    void signedAmountDependsOnType() {
        assertEquals(500L, new Transaction(1, 1, "a", 500L, TransactionType.INCOME).signedAmount());
        assertEquals(-500L, new Transaction(2, 1, "b", 500L, TransactionType.EXPENSE).signedAmount());
    }

    @Test
    void mutatorsChangeFields() {
        Transaction transaction = new Transaction(1, 1, "a", 100L, TransactionType.INCOME);
        transaction.changeDescription("baru");
        transaction.changeAmount(900L);
        transaction.changeProofPath("proofs/b.png");

        assertEquals("baru", transaction.getDescription());
        assertEquals(900L, transaction.getAmount());
        assertEquals("proofs/b.png", transaction.getProofPath());
    }

    @Test
    void typeLabels() {
        assertEquals("Pemasukan", TransactionType.INCOME.label());
        assertEquals("Pengeluaran", TransactionType.EXPENSE.label());
    }
}
