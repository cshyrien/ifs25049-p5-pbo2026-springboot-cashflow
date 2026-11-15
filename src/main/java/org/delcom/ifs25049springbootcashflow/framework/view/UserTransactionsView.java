package org.delcom.ifs25049springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25049springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25049springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25049springbootcashflow.domain.entity.User;
import org.delcom.ifs25049springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25049springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25049springbootcashflow.usecase.FinanceUseCase;
import org.delcom.ifs25049springbootcashflow.usecase.UserUseCase;

/**
 * Halaman khusus admin: melihat (hanya baca) transaksi milik pengguna lain.
 */
@Route(value = "user-transactions", layout = MainLayout.class)
@PageTitle("Transaksi Pengguna")
public class UserTransactionsView extends VerticalLayout implements HasUrlParameter<Integer>, BeforeEnterObserver {

    private final UserUseCase userUseCase;
    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final H1 title = new H1("Transaksi Pengguna");
    private final Paragraph balance = new Paragraph();
    private final Paragraph empty = new Paragraph("Belum ada transaksi.");

    public UserTransactionsView(
            UserUseCase userUseCase,
            FinanceUseCase financeUseCase,
            FinancePresenter presenter
    ) {
        this.userUseCase = userUseCase;
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set("background", "#F3F4F6");

        title.getStyle().set("margin", "0");
        balance.getStyle().set("margin", "0").set("font-weight", "600").set("color", "#1E40AF");
        empty.getStyle().set("margin", "0").set("color", "#4B5563");

        grid.getElement().setAttribute("aria-label", "Daftar transaksi pengguna");
        grid.addComponentColumn(transaction -> {
            if (transaction.getProofPath() == null || transaction.getProofPath().isBlank()) {
                return new Span("-");
            }
            return ImagePreviewDialog.clickableThumbnail(
                    "/uploads/" + transaction.getProofPath(),
                    "Bukti",
                    "48px"
            );
        }).setHeader("Bukti").setAutoWidth(true);
        grid.addColumn(Transaction::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setFlexGrow(1);
        grid.addColumn(transaction -> presenter.formatMoney(transaction.getAmount()))
                .setHeader("Jumlah").setAutoWidth(true);
        grid.addColumn(transaction -> transaction.getType().label()).setHeader("Tipe").setAutoWidth(true);
        grid.addColumn(presenter::formatCreatedAt).setHeader("Dibuat").setAutoWidth(true);
        grid.setSizeFull();
        GridStyles.apply(grid);

        Div card = PageCard.wrap(title, balance, empty, grid);
        add(card);
        expand(card);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            event.forwardTo(FinanceView.class);
        }
    }

    @Override
    public void setParameter(com.vaadin.flow.router.BeforeEvent event, Integer userId) {
        User current = AuthSession.currentUser().orElse(null);
        if (current == null || !current.isAdmin()) {
            return;
        }
        try {
            User owner = userUseCase.getUser(userId);
            title.setText("Transaksi milik " + owner.getFullName() + " (@" + owner.getUsername() + ")");
            var transactions = financeUseCase.getTransactionsByUser(userId);
            balance.setText(presenter.formatBalance(financeUseCase.getBalance(userId)));
            grid.setItems(transactions);
            empty.setVisible(transactions.isEmpty());
            grid.setVisible(!transactions.isEmpty());
        } catch (EntityNotFoundException ex) {
            event.forwardTo(UsersView.class);
        }
    }
}
