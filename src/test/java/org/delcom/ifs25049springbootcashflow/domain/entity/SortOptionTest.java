package org.delcom.ifs25049springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortOptionTest {

    private List<Transaction> sample() {
        List<Transaction> list = new ArrayList<>();
        list.add(new Transaction(1, 1, "a", 300L, TransactionType.EXPENSE));
        list.add(new Transaction(2, 1, "b", 100L, TransactionType.INCOME));
        list.add(new Transaction(3, 1, "c", 200L, TransactionType.INCOME));
        return list;
    }

    @Test
    void amountAscending() {
        List<Transaction> list = sample();
        list.sort(SortOption.AMOUNT_ASC.comparator());
        assertEquals(100L, list.get(0).getAmount());
        assertEquals(300L, list.get(2).getAmount());
    }

    @Test
    void amountDescending() {
        List<Transaction> list = sample();
        list.sort(SortOption.AMOUNT_DESC.comparator());
        assertEquals(300L, list.get(0).getAmount());
        assertEquals(100L, list.get(2).getAmount());
    }

    @Test
    void incomeFirst() {
        List<Transaction> list = sample();
        list.sort(SortOption.INCOME_FIRST.comparator());
        assertEquals(TransactionType.INCOME, list.get(0).getType());
        assertEquals(TransactionType.EXPENSE, list.get(2).getType());
    }

    @Test
    void expenseFirst() {
        List<Transaction> list = sample();
        list.sort(SortOption.EXPENSE_FIRST.comparator());
        assertEquals(TransactionType.EXPENSE, list.get(0).getType());
        assertEquals(TransactionType.INCOME, list.get(2).getType());
    }

    @Test
    void labelsAreDefined() {
        for (SortOption option : SortOption.values()) {
            assertEquals(false, option.label().isBlank());
        }
    }
}
