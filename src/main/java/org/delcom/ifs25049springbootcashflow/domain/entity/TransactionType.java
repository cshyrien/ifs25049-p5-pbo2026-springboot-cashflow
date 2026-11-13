package org.delcom.ifs25049springbootcashflow.domain.entity;

/**
 * Jenis transaksi keuangan.
 */
public enum TransactionType {
    /** Uang masuk (menambah saldo). */
    INCOME("Pemasukan"),

    /** Uang keluar (mengurangi saldo). */
    EXPENSE("Pengeluaran");

    private final String label;

    TransactionType(String label) {
        this.label = label;
    }

    /** Label yang ditampilkan ke pengguna. */
    public String label() {
        return label;
    }
}
