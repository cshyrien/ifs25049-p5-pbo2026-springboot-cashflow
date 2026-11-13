package org.delcom.ifs25049springbootcashflow.usecase;

import org.delcom.ifs25049springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25049springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25049springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25049springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Optional;

/**
 * Use case yang menangani logika bisnis catatan keuangan (cashflow).
 * Tidak melakukan I/O: hanya memproses data dan melempar checked exception
 * domain jika operasi tidak valid atau data tidak ditemukan.
 */
public class FinanceUseCase {
    /** Port repository yang di-inject dari luar (Dependency Inversion). */
    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Mengambil semua transaksi milik satu pengguna. */
    public List<Transaction> getTransactionsByUser(int userId) {
        return transactionRepository.findByUserId(userId);
    }

    /** Mengambil semua transaksi yang tersedia (admin). */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /** Menambahkan pemasukan baru setelah validasi. */
    public Transaction addIncome(int userId, String description, long amount) throws ValidationException {
        return addTransaction(userId, description, amount, TransactionType.INCOME);
    }

    /** Menambahkan pengeluaran baru setelah validasi. */
    public Transaction addExpense(int userId, String description, long amount) throws ValidationException {
        return addTransaction(userId, description, amount, TransactionType.EXPENSE);
    }

    private Transaction addTransaction(int userId, String description, long amount, TransactionType type)
            throws ValidationException {
        String safeDescription = requireDescription(description);
        requirePositiveAmount(amount);
        return transactionRepository.save(userId, safeDescription, amount, type);
    }

    /**
     * Mengubah teks jumlah dari input pengguna menjadi angka.
     * Menolak teks kosong, non-numerik, dan jumlah yang tidak positif.
     */
    public long parseAmount(String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Jumlah tidak boleh kosong");
        }
        long amount;
        try {
            amount = Long.parseLong(text.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Jumlah harus berupa angka");
        }
        requirePositiveAmount(amount);
        return amount;
    }

    /** Menghapus transaksi berdasarkan ID; melempar exception jika tidak ditemukan. */
    public void removeTransaction(int id) throws EntityNotFoundException {
        boolean deleted = transactionRepository.deleteById(id);
        if (!deleted) {
            throw new EntityNotFoundException(id);
        }
    }

    /** Menghapus transaksi milik pengguna tertentu. */
    public void removeTransactionForUser(int userId, int id) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        removeTransaction(transaction.getId());
    }

    /**
     * Mengubah keterangan, jumlah, dan/atau bukti transaksi.
     * Parameter {@code null} berarti field tersebut tidak diubah
     * (kecuali {@code clearProof=true} untuk menghapus bukti).
     */
    public void updateTransaction(
            int id,
            String description,
            Long amount,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }

        Transaction transaction = found.get();
        applyUpdates(transaction, description, amount, proofPath, clearProof);
        transactionRepository.update(transaction);
    }

    public void updateTransactionForUser(
            int userId,
            int id,
            String description,
            Long amount,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        applyUpdates(transaction, description, amount, proofPath, clearProof);
        transactionRepository.update(transaction);
    }

    /** Mencari transaksi pengguna yang keterangannya mengandung kata kunci (case-insensitive). */
    public List<Transaction> searchTransactions(int userId, String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return transactionRepository.findByUserId(userId).stream()
                .filter(transaction -> transaction.getDescription().toLowerCase().contains(lowerKeyword))
                .toList();
    }

    /** Mengurutkan transaksi pengguna sesuai kriteria {@link SortOption} yang dipilih. */
    public List<Transaction> sortTransactions(int userId, SortOption option) {
        return transactionRepository.findByUserId(userId).stream()
                .sorted(option.comparator())
                .toList();
    }

    /** Total seluruh pemasukan milik pengguna. */
    public long getTotalIncome(int userId) {
        return sumByType(userId, TransactionType.INCOME);
    }

    /** Total seluruh pengeluaran milik pengguna. */
    public long getTotalExpense(int userId) {
        return sumByType(userId, TransactionType.EXPENSE);
    }

    /** Saldo = total pemasukan - total pengeluaran. */
    public long getBalance(int userId) {
        return getTotalIncome(userId) - getTotalExpense(userId);
    }

    private long sumByType(int userId, TransactionType type) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(transaction -> transaction.getType() == type)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    private Transaction requireOwnedTransaction(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }
        Transaction transaction = found.get();
        if (transaction.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak milik pengguna ini");
        }
        return transaction;
    }

    private void applyUpdates(
            Transaction transaction,
            String description,
            Long amount,
            String proofPath,
            boolean clearProof
    ) throws ValidationException {
        if (description != null) {
            transaction.changeDescription(requireDescription(description));
        }
        if (amount != null) {
            requirePositiveAmount(amount);
            transaction.changeAmount(amount);
        }
        if (clearProof) {
            transaction.changeProofPath(null);
        } else if (proofPath != null) {
            transaction.changeProofPath(proofPath);
        }
    }

    private static String requireDescription(String description) throws ValidationException {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
        return description.trim();
    }

    private static void requirePositiveAmount(long amount) throws ValidationException {
        if (amount <= 0) {
            throw new ValidationException("Jumlah harus lebih dari 0");
        }
    }
}
