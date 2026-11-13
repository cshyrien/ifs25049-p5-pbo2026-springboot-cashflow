package org.delcom.ifs25049springbootcashflow.usecase;

import org.delcom.ifs25049springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25049springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25049springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25049springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceUseCaseTest {

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FinanceUseCase(new TransactionRepository());
    }

    @Test
    void addIncomeAndExpenseTrimDescription() throws Exception {
        Transaction income = useCase.addIncome(1, "  Gaji  ", 5000000L);
        Transaction expense = useCase.addExpense(1, "Makan", 25000L);

        assertEquals("Gaji", income.getDescription());
        assertEquals(TransactionType.INCOME, income.getType());
        assertEquals(TransactionType.EXPENSE, expense.getType());
        assertEquals(2, useCase.getTransactionsByUser(1).size());
        assertEquals(2, useCase.getAllTransactions().size());
    }

    @Test
    void addRejectsBlankDescription() {
        assertThrows(ValidationException.class, () -> useCase.addIncome(1, "   ", 100L));
        assertThrows(ValidationException.class, () -> useCase.addExpense(1, null, 100L));
    }

    @Test
    void addRejectsNonPositiveAmount() {
        assertThrows(ValidationException.class, () -> useCase.addIncome(1, "a", 0L));
        assertThrows(ValidationException.class, () -> useCase.addExpense(1, "a", -5L));
    }

    @Test
    void parseAmountValidatesInput() throws Exception {
        assertEquals(1000L, useCase.parseAmount(" 1000 "));
        assertThrows(ValidationException.class, () -> useCase.parseAmount(null));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("  "));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("abc"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("12.5"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("0"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("-10"));
    }

    @Test
    void balanceIsIncomeMinusExpense() throws Exception {
        useCase.addIncome(1, "Gaji", 5000000L);
        useCase.addExpense(1, "Makan", 25000L);
        useCase.addIncome(2, "Lain", 999L);

        assertEquals(5000000L, useCase.getTotalIncome(1));
        assertEquals(25000L, useCase.getTotalExpense(1));
        assertEquals(4975000L, useCase.getBalance(1));
        assertEquals(0L, useCase.getBalance(99));
    }

    @Test
    void balanceCanBeNegative() throws Exception {
        useCase.addExpense(1, "Beli laptop", 10000000L);
        assertEquals(-10000000L, useCase.getBalance(1));
    }

    @Test
    void removeTransaction() throws Exception {
        Transaction saved = useCase.addIncome(1, "a", 100L);
        useCase.removeTransaction(saved.getId());
        assertTrue(useCase.getAllTransactions().isEmpty());
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(saved.getId()));
    }

    @Test
    void removeForUserChecksOwnership() throws Exception {
        Transaction saved = useCase.addIncome(1, "a", 100L);

        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(2, saved.getId()));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransactionForUser(1, 999));
        useCase.removeTransactionForUser(1, saved.getId());
        assertTrue(useCase.getAllTransactions().isEmpty());
    }

    @Test
    void updateTransactionChangesFields() throws Exception {
        Transaction saved = useCase.addIncome(1, "a", 100L);

        useCase.updateTransaction(saved.getId(), "baru", 900L, "proofs/x.png", false);
        Transaction updated = useCase.getTransactionsByUser(1).get(0);
        assertEquals("baru", updated.getDescription());
        assertEquals(900L, updated.getAmount());
        assertEquals("proofs/x.png", updated.getProofPath());

        useCase.updateTransaction(saved.getId(), null, null, null, true);
        assertNull(useCase.getTransactionsByUser(1).get(0).getProofPath());

        useCase.updateTransaction(saved.getId(), null, null, null, false);
        assertEquals("baru", useCase.getTransactionsByUser(1).get(0).getDescription());
    }

    @Test
    void updateTransactionValidates() throws Exception {
        Transaction saved = useCase.addIncome(1, "a", 100L);

        assertThrows(EntityNotFoundException.class, () -> useCase.updateTransaction(999, "x", 1L, null, false));
        assertThrows(ValidationException.class, () -> useCase.updateTransaction(saved.getId(), " ", null, null, false));
        assertThrows(ValidationException.class, () -> useCase.updateTransaction(saved.getId(), null, 0L, null, false));
    }

    @Test
    void updateForUserChecksOwnership() throws Exception {
        Transaction saved = useCase.addIncome(1, "a", 100L);

        assertThrows(ValidationException.class,
                () -> useCase.updateTransactionForUser(2, saved.getId(), "x", null, null, false));
        assertThrows(EntityNotFoundException.class,
                () -> useCase.updateTransactionForUser(1, 999, "x", null, null, false));
        useCase.updateTransactionForUser(1, saved.getId(), "ok", 250L, null, false);
        assertEquals("ok", useCase.getTransactionsByUser(1).get(0).getDescription());
    }

    @Test
    void searchIsCaseInsensitive() throws Exception {
        useCase.addIncome(1, "Gaji Bulanan", 100L);
        useCase.addExpense(1, "Makan siang", 50L);

        assertEquals(1, useCase.searchTransactions(1, "GAJI").size());
        assertTrue(useCase.searchTransactions(1, "tidakada").isEmpty());
    }

    @Test
    void sortTransactionsUsesOption() throws Exception {
        useCase.addIncome(1, "a", 300L);
        useCase.addExpense(1, "b", 100L);
        useCase.addIncome(1, "c", 200L);

        List<Transaction> ascending = useCase.sortTransactions(1, SortOption.AMOUNT_ASC);
        assertEquals(100L, ascending.get(0).getAmount());
        List<Transaction> descending = useCase.sortTransactions(1, SortOption.AMOUNT_DESC);
        assertEquals(300L, descending.get(0).getAmount());
        assertEquals(TransactionType.EXPENSE,
                useCase.sortTransactions(1, SortOption.EXPENSE_FIRST).get(0).getType());
        assertEquals(TransactionType.INCOME,
                useCase.sortTransactions(1, SortOption.INCOME_FIRST).get(0).getType());
    }
}
