package org.delcom.ifs25049springbootcashflow.adapter.presenter;

import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancePresenterTest {

    private final FinancePresenter presenter = new FinancePresenter();

    private Transaction sample() {
        return new Transaction(1, 1, "Gaji", 1000000L, TransactionType.INCOME, null,
                LocalDateTime.of(2026, 11, 13, 9, 5));
    }

    @Test
    void formatMoneyUsesCommaThousandsAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", presenter.formatMoney(1000000L));
        assertEquals("Rp 0.00", presenter.formatMoney(0L));
        assertEquals("Rp 500.00", presenter.formatMoney(500L));
    }

    @Test
    void formatMoneyHandlesNegativeBalance() {
        assertEquals("-Rp 25,000.00", presenter.formatMoney(-25000L));
    }

    @Test
    void formatSingleTransaction() {
        assertEquals("[1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.format(sample()));
    }

    @Test
    void formatExpenseTransaction() {
        Transaction expense = new Transaction(2, 1, "Makan", 25000L, TransactionType.EXPENSE);
        assertEquals("[2] Makan | Rp 25,000.00 | [Pengeluaran]", presenter.format(expense));
    }

    @Test
    void formatCreatedAtAndBalance() {
        assertEquals("13/11/2026 09:05", presenter.formatCreatedAt(sample()));
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4975000L));
    }

    @Test
    void listsAndEmptyMessages() {
        assertTrue(presenter.showTransactions(List.of()).contains("belum tersedia"));
        assertTrue(presenter.showTransactions(List.of(sample())).contains("[1] Gaji"));
        assertTrue(presenter.showSearchResults(List.of(), "x").contains("tidak ditemukan"));
        assertTrue(presenter.showSearchResults(List.of(sample()), "gaji").contains("gaji"));
        assertTrue(presenter.showSortedTransactions(List.of()).contains("belum tersedia"));
        assertTrue(presenter.showSortedTransactions(List.of(sample())).contains("Terurut"));
    }

    @Test
    void messages() {
        assertTrue(presenter.showAddSuccess(sample()).startsWith("Berhasil menambah transaksi"));
        assertEquals("Berhasil menghapus transaksi.", presenter.showRemoveSuccess());
        assertEquals("[!] Gagal menghapus transaksi dengan ID: 5.", presenter.showRemoveFailed(5));
        assertEquals("Berhasil mengubah transaksi.", presenter.showUpdateSuccess());
        assertEquals("[!] Gagal mengubah transaksi dengan ID: 5.", presenter.showUpdateFailed(5));
        assertEquals("[!] ID tidak valid!", presenter.showInvalidId());
        assertEquals("[!] Pilihan tidak valid!", presenter.showInvalidSortOption());
        assertEquals("[!] salah", presenter.showValidationError("salah"));
    }
}
