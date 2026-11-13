package org.delcom.ifs25049springbootcashflow.domain.repository;

import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.TransactionType;

import java.util.List;

/**
 * Port spesifik Transaction yang memperluas generic {@link IRepository}.
 * Menambahkan method pembuatan transaksi baru dengan penomoran ID otomatis.
 */
public interface ITransactionRepository extends IRepository<Transaction, Integer> {
    /**
     * Menyimpan transaksi baru untuk pengguna tertentu.
     */
    Transaction save(int userId, String description, long amount, TransactionType type);

    /** Mengambil semua transaksi milik satu pengguna. */
    List<Transaction> findByUserId(int userId);
}
