package org.delcom.ifs25049springbootcashflow.domain.entity;

import java.util.Comparator;

/**
 * Kriteria pengurutan transaksi.
 * Setiap opsi membawa comparator-nya sendiri sehingga logika pengurutan
 * terpusat di domain, bukan tersebar sebagai angka ajaib di view/use case.
 */
public enum SortOption {
    /** Jumlah dari yang terkecil ke terbesar. */
    AMOUNT_ASC("Jumlah naik", Comparator.comparingLong(Transaction::getAmount)),

    /** Jumlah dari yang terbesar ke terkecil. */
    AMOUNT_DESC("Jumlah turun", Comparator.comparingLong(Transaction::getAmount).reversed()),

    /** Pemasukan ditampilkan lebih dulu (INCOME sebelum EXPENSE). */
    INCOME_FIRST("Pemasukan dulu", Comparator.comparing(Transaction::getType)),

    /** Pengeluaran ditampilkan lebih dulu (EXPENSE sebelum INCOME). */
    EXPENSE_FIRST("Pengeluaran dulu", Comparator.comparing(Transaction::getType).reversed());

    private final String label;

    /** Comparator yang digunakan untuk mengurutkan daftar transaksi. */
    private final Comparator<Transaction> comparator;

    SortOption(String label, Comparator<Transaction> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    /** Label yang ditampilkan ke pengguna. */
    public String label() {
        return label;
    }

    /** Mengembalikan comparator yang sesuai dengan opsi ini. */
    public Comparator<Transaction> comparator() {
        return comparator;
    }
}
