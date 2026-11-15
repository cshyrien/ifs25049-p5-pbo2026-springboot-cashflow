package org.delcom.ifs25049springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25049springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25049springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25049springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25049springbootcashflow.domain.entity.User;
import org.delcom.ifs25049springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25049springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25049springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25049springbootcashflow.usecase.FinanceUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tampilan web Vaadin catatan keuangan milik pengguna yang sedang login.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Cashflow")
public class FinanceView extends VerticalLayout {

    private static final String INCOME_COLOR = "#15803D";
    private static final String EXPENSE_COLOR = "#DC2626";

    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final FileStorage fileStorage;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<SortOption> sortField = new ComboBox<>();
    private final Paragraph statusLabel = new Paragraph();
    private final Span emptyState = new Span("- Data transaksi belum tersedia!");
    private final Span totalCount = new Span("0");
    private final Span incomeTotal = new Span("Rp 0.00");
    private final Span expenseTotal = new Span("Rp 0.00");
    private final Span balanceTotal = new Span("Rp 0.00");

    public FinanceView(FinanceUseCase financeUseCase, FinancePresenter presenter, FileStorage fileStorage) {
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        H1 title = new H1("Cashflow Saya");
        title.getStyle().set("margin", "0");

        configureGrid();
        configureToolbar();

        Div card = PageCard.wrap(title, buildSummaryCards(), buildToolbar(), statusLabel, grid);
        add(card);
        expand(card);
        refreshAll();
    }

    private User requireUser() {
        return AuthSession.currentUser().orElseThrow();
    }

    private HorizontalLayout buildSummaryCards() {
        HorizontalLayout summary = new HorizontalLayout(
                createSummaryCard("Total Transaksi", totalCount, "#DBEAFE", "#1D4ED8", "#1E40AF"),
                createSummaryCard("Total Pemasukan", incomeTotal, "#DCFCE7", "#16A34A", "#166534"),
                createSummaryCard("Total Pengeluaran", expenseTotal, "#FEE2E2", "#DC2626", "#991B1B"),
                createSummaryCard("Saldo", balanceTotal, "#EDE9FE", "#7C3AED", "#5B21B6")
        );
        summary.setWidthFull();
        summary.setSpacing(true);
        summary.getStyle().set("align-items", "stretch");
        return summary;
    }

    private Div createSummaryCard(
            String label,
            Span value,
            String background,
            String accent,
            String valueColor
    ) {
        Span caption = new Span(label);
        caption.getStyle()
                .set("display", "block")
                .set("font-size", "0.875rem")
                .set("font-weight", "500")
                .set("color", valueColor);

        value.getStyle()
                .set("display", "block")
                .set("margin-top", "0.35rem")
                .set("font-size", "1.5rem")
                .set("font-weight", "700")
                .set("line-height", "1.1")
                .set("color", valueColor);

        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1")
                .set("min-width", "0")
                .set("background", background)
                .set("border", "1px solid " + accent)
                .set("border-left", "6px solid " + accent)
                .set("border-radius", "12px")
                .set("padding", "1rem 1.1rem")
                .set("box-sizing", "border-box");
        return card;
    }

    private void updateSummary() {
        int userId = requireUser().getId();
        totalCount.setText(String.valueOf(financeUseCase.getTransactionsByUser(userId).size()));
        incomeTotal.setText(presenter.formatMoney(financeUseCase.getTotalIncome(userId)));
        expenseTotal.setText(presenter.formatMoney(financeUseCase.getTotalExpense(userId)));
        balanceTotal.setText(presenter.formatMoney(financeUseCase.getBalance(userId)));
    }

    private void configureGrid() {
        grid.getElement().setAttribute("aria-label", "Daftar transaksi keuangan");
        grid.addComponentColumn(this::createProofPreview)
                .setHeader("Bukti")
                .setKey("proof")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);

        grid.addColumn(Transaction::getId)
                .setHeader("ID")
                .setKey("id")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true)
                .setComparator(Transaction::getId);

        grid.addColumn(Transaction::getDescription)
                .setHeader("Keterangan")
                .setKey("description")
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getDescription, String.CASE_INSENSITIVE_ORDER));

        grid.addComponentColumn(this::createAmountLabel)
                .setHeader("Jumlah")
                .setKey("amount")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparingLong(Transaction::getAmount));

        grid.addColumn(transaction -> transaction.getType().label())
                .setHeader("Tipe")
                .setKey("type")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getType));

        grid.addColumn(presenter::formatCreatedAt)
                .setHeader("Dibuat")
                .setKey("createdAt")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getCreatedAt));

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Tindakan")
                .setKey("tindakan")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);

        grid.setEmptyStateComponent(emptyState);
        grid.addItemDoubleClickListener(event -> openEditDialog(event.getItem()));
        grid.setSizeFull();
        GridStyles.apply(grid);
    }

    private Component createProofPreview(Transaction transaction) {
        if (transaction.getProofPath() == null || transaction.getProofPath().isBlank()) {
            return new Span("-");
        }
        return ImagePreviewDialog.clickableThumbnail(
                "/uploads/" + transaction.getProofPath(),
                "Bukti",
                "48px"
        );
    }

    private Span createAmountLabel(Transaction transaction) {
        String sign = transaction.isIncome() ? "+ " : "- ";
        Span label = new Span(sign + presenter.formatMoney(transaction.getAmount()));
        label.getStyle()
                .set("font-weight", "600")
                .set("color", transaction.isIncome() ? INCOME_COLOR : EXPENSE_COLOR);
        return label;
    }

    private HorizontalLayout createActionButtons(Transaction transaction) {
        Button deleteButton = new Button("Hapus", VaadinIcon.TRASH.create(), event -> openDeleteDialog(transaction));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteButton.getStyle().set("cursor", "pointer");
        return new HorizontalLayout(deleteButton);
    }

    private void configureToolbar() {
        // Placeholder saja tidak cukup sebagai nama aksesibel untuk pembaca layar.
        searchField.setLabel("Cari transaksi");
        searchField.setPlaceholder("Cari keterangan...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);

        sortField.setLabel("Urutkan transaksi");
        sortField.setPlaceholder("Urutkan");
        sortField.setItems(SortOption.values());
        sortField.setItemLabelGenerator(SortOption::label);
        sortField.setClearButtonVisible(true);
        sortField.addValueChangeListener(event -> {
            if (event.getValue() == null) {
                refreshAll();
            } else {
                sortTransactions(event.getValue());
            }
        });
    }

    private HorizontalLayout buildToolbar() {
        Button incomeButton = new Button("Pemasukan", VaadinIcon.ARROW_DOWN.create(),
                event -> openAddDialog(TransactionType.INCOME));
        incomeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        incomeButton.getStyle().set("cursor", "pointer");

        Button expenseButton = new Button("Pengeluaran", VaadinIcon.ARROW_UP.create(),
                event -> openAddDialog(TransactionType.EXPENSE));
        expenseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        expenseButton.getStyle().set("cursor", "pointer");

        Button searchButton = new Button("Cari", VaadinIcon.SEARCH.create(), event -> searchTransactions());
        searchButton.getStyle().set("cursor", "pointer");

        Button refreshButton = new Button("Muat Ulang", VaadinIcon.REFRESH.create(), event -> refreshAll());
        refreshButton.getStyle().set("cursor", "pointer");

        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                searchButton,
                sortField,
                refreshButton,
                incomeButton,
                expenseButton
        );
        toolbar.setWidthFull();
        toolbar.getStyle().set("flex-wrap", "wrap");
        toolbar.setFlexGrow(1, searchField);
        return toolbar;
    }

    private void openAddDialog(TransactionType type) {
        TextField descriptionField = new TextField("Keterangan");
        TextField amountField = new TextField("Jumlah (Rp)");
        amountField.setPlaceholder("contoh: 1000000");
        descriptionField.setWidthFull();
        amountField.setWidthFull();

        AtomicReference<String> pendingProofName = new AtomicReference<>();
        AtomicReference<byte[]> pendingProofData = new AtomicReference<>();
        Upload upload = imageUpload(pendingProofName, pendingProofData);
        Span uploadHint = new Span("Bukti transaksi (opsional)");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            try {
                User user = requireUser();
                long amount = financeUseCase.parseAmount(amountField.getValue());
                Transaction created = type == TransactionType.INCOME
                        ? financeUseCase.addIncome(user.getId(), descriptionField.getValue(), amount)
                        : financeUseCase.addExpense(user.getId(), descriptionField.getValue(), amount);
                String filename = pendingProofName.get();
                byte[] data = pendingProofData.get();
                if (filename != null && !filename.isBlank() && data != null) {
                    String path = fileStorage.store("proofs", filename, new ByteArrayInputStream(data));
                    financeUseCase.updateTransactionForUser(user.getId(), created.getId(), null, null, path, false);
                }
                showSuccess(presenter.showAddSuccess(created));
                dialog.close();
                refreshAll();
            } catch (ValidationException | EntityNotFoundException | IOException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        VerticalLayout body = new VerticalLayout(descriptionField, amountField, uploadHint, upload);
        body.setPadding(false);
        body.setSpacing(true);
        String title = type == TransactionType.INCOME ? "Menambah Pemasukan" : "Menambah Pengeluaran";
        buildDialog(dialog, title, loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private void openEditDialog(Transaction transaction) {
        TextField descriptionField = new TextField("Keterangan");
        descriptionField.setValue(transaction.getDescription() == null ? "" : transaction.getDescription());
        TextField amountField = new TextField("Jumlah (Rp)");
        amountField.setValue(String.valueOf(transaction.getAmount()));
        descriptionField.setWidthFull();
        amountField.setWidthFull();

        AtomicReference<String> pendingProofName = new AtomicReference<>();
        AtomicReference<byte[]> pendingProofData = new AtomicReference<>();
        Upload upload = imageUpload(pendingProofName, pendingProofData);
        Span uploadHint = new Span("Ganti bukti (opsional)");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            try {
                User user = requireUser();
                long amount = financeUseCase.parseAmount(amountField.getValue());
                String proofPath = null;
                String filename = pendingProofName.get();
                byte[] data = pendingProofData.get();
                if (filename != null && !filename.isBlank() && data != null) {
                    proofPath = fileStorage.store("proofs", filename, new ByteArrayInputStream(data));
                }
                financeUseCase.updateTransactionForUser(
                        user.getId(),
                        transaction.getId(),
                        descriptionField.getValue(),
                        amount,
                        proofPath,
                        false
                );
                showSuccess(presenter.showUpdateSuccess());
                dialog.close();
                refreshAll();
            } catch (EntityNotFoundException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showUpdateFailed(ex.getId()));
            } catch (ValidationException | IOException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        VerticalLayout body = new VerticalLayout(descriptionField, amountField, uploadHint, upload);
        body.setPadding(false);
        body.setSpacing(true);
        buildDialog(dialog, "Mengubah Transaksi #" + transaction.getId(), loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private void openDeleteDialog(Transaction transaction) {
        Paragraph message = new Paragraph(
                "Yakin ingin menghapus transaksi \"" + transaction.getDescription()
                        + "\" (ID " + transaction.getId() + ")?"
        );
        message.getStyle().set("margin", "0");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button confirmButton = new Button("Hapus", VaadinIcon.TRASH.create());
        confirmButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            confirmButton.setEnabled(false);
            deleteTransaction(transaction);
            dialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        buildDialog(dialog, "Menghapus Transaksi", loadingBar, message, cancelButton, confirmButton);
        dialog.open();
    }

    /**
     * Membangun dialog modal: header primer (#2563EB) berteks putih,
     * body putih, dan footer abu-abu (#F3F4F6) dengan tombol rata kanan.
     */
    private void buildDialog(
            Dialog dialog,
            String title,
            ProgressBar loadingBar,
            Component bodyContent,
            Button... footerButtons
    ) {
        dialog.setWidth("500px");
        dialog.setMaxWidth("95vw");
        dialog.setResizable(false);
        dialog.addClassName("finance-form-dialog");

        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set("margin", "0")
                .set("color", "#FFFFFF")
                .set("font-size", "1.125rem")
                .set("font-weight", "600")
                .set("line-height", "1.4");

        Div header = new Div(headerTitle);
        header.getStyle()
                .set("background", "#2563EB")
                .set("padding", "1rem 1.5rem")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        loadingBar.setIndeterminate(true);
        loadingBar.getStyle().set("margin", "0").set("height", "4px");

        Div body = new Div(bodyContent);
        body.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "1.25rem 1.5rem")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("align-items", "center")
                .set("gap", "0.75rem")
                .set("background", "#F3F4F6")
                .set("border-top", "1px solid #E5E7EB")
                .set("padding", "0.85rem 1.5rem 1.1rem")
                .set("box-sizing", "border-box")
                .set("width", "100%");
        for (Button btn : footerButtons) {
            btn.getStyle().set("cursor", "pointer");
        }
        footer.add(footerButtons);

        Div shell = new Div(header, loadingBar, body, footer);
        shell.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("width", "100%")
                .set("overflow", "hidden")
                .set("border-radius", "12px")
                .set("background", "#FFFFFF");

        dialog.removeAll();
        dialog.add(shell);
        removeOverlayPadding(dialog);
    }

    private void removeOverlayPadding(Dialog dialog) {
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                dialog.getElement().executeJs("""
                        const overlay = this.$.overlay;
                        if (!overlay || !overlay.shadowRoot) {
                          return;
                        }
                        const content = overlay.shadowRoot.querySelector('[part="content"]');
                        if (content) {
                          content.style.padding = '0';
                          content.style.overflow = 'visible';
                          content.style.maxHeight = 'none';
                        }
                        const overlayPart = overlay.shadowRoot.querySelector('[part="overlay"]');
                        if (overlayPart) {
                          overlayPart.style.padding = '0';
                          overlayPart.style.overflow = 'hidden';
                        }
                        """);
            }
        });
    }

    private void deleteTransaction(Transaction transaction) {
        try {
            financeUseCase.removeTransactionForUser(requireUser().getId(), transaction.getId());
            showSuccess(presenter.showRemoveSuccess());
            refreshAll();
        } catch (EntityNotFoundException ex) {
            showError(presenter.showRemoveFailed(ex.getId()));
        } catch (ValidationException ex) {
            showError(presenter.showValidationError(ex.getMessage()));
        }
    }

    private void searchTransactions() {
        String keyword = searchField.getValue();
        if (keyword.isBlank()) {
            refreshAll();
            return;
        }
        List<Transaction> results = financeUseCase.searchTransactions(requireUser().getId(), keyword);
        emptyState.setText(results.isEmpty()
                ? "- Transaksi tidak ditemukan!"
                : "- Data transaksi belum tersedia!");
        refreshGrid(results, "Hasil Pencarian: \"" + keyword + "\"");
    }

    private void sortTransactions(SortOption option) {
        emptyState.setText("- Data transaksi belum tersedia!");
        refreshGrid(
                financeUseCase.sortTransactions(requireUser().getId(), option),
                "Daftar Transaksi (Terurut: " + option.label() + ")"
        );
    }

    private void refreshAll() {
        emptyState.setText("- Data transaksi belum tersedia!");
        refreshGrid(financeUseCase.getTransactionsByUser(requireUser().getId()), "Daftar Transaksi");
        searchField.clear();
        sortField.clear();
    }

    private void refreshGrid(List<Transaction> transactions, String title) {
        grid.setItems(transactions);
        updateSummary();
        if (transactions.isEmpty()) {
            statusLabel.setText(title + " — " + emptyState.getText().replaceFirst("^- ", ""));
        } else {
            statusLabel.setText(title);
        }
    }

    private Upload imageUpload(AtomicReference<String> pendingName, AtomicReference<byte[]> pendingData) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.getElement().setAttribute("aria-label", "Unggah bukti transaksi opsional");
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        return upload;
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
