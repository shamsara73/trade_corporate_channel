package com.bank.tradechannel.persistence;

import static com.bank.tradechannel.persistence.Db.id;

import com.bank.tradechannel.domain.finance.FinanceRequest;
import com.bank.tradechannel.domain.finance.FsaDetailStore;
import com.bank.tradechannel.domain.finance.InterestFrom;
import com.bank.tradechannel.domain.finance.Money;
import com.bank.tradechannel.domain.finance.RepaymentAction;
import com.bank.tradechannel.domain.finance.RepaymentPayer;
import com.bank.tradechannel.domain.finance.RepaymentRequest;
import java.time.LocalDate;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Tables fsa_create_det and fsa_repay_det. */
public final class JdbcFsaDetailStore implements FsaDetailStore {
  private final JdbcClient jdbc;

  public JdbcFsaDetailStore(JdbcClient jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void saveCreate(UUID eventId, FinanceRequest r) {
    jdbc.sql(
            "INSERT INTO fsa_create_det (event_id, their_reference, currency_code, finance_amount,"
                + " txn_amount, start_date, tenor_days, maturity_date, trust_receipt, interest_from,"
                + " product_type, goods_desc, message_to_bank, additional_info)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(eventId), r.theirReference(), r.financeAmount().currencyCode(),
            r.financeAmount().amount(), r.transactionAmount().amount(), r.startDate(), r.tenorDays(),
            r.maturityDate(), r.trustReceiptLoan() ? "Y" : "N", r.interestFrom().name(),
            r.productType(), Db.orNull(r.goodsDescription()), Db.orNull(r.messageToBank()),
            Db.orNull(r.additionalInformation()))
        .update();
  }

  @Override
  public Optional<FinanceRequest> create(UUID eventId) {
    return jdbc.sql("SELECT * FROM fsa_create_det WHERE event_id = ?")
        .param(id(eventId))
        .query(
            (rs, n) -> {
              Currency currency = Currency.getInstance(rs.getString("currency_code").trim());
              return new FinanceRequest(
                  rs.getString("their_reference"),
                  new Money(rs.getBigDecimal("finance_amount"), currency),
                  new Money(rs.getBigDecimal("txn_amount"), currency),
                  rs.getObject("start_date", LocalDate.class),
                  rs.getInt("tenor_days"),
                  "Y".equals(rs.getString("trust_receipt")),
                  InterestFrom.valueOf(rs.getString("interest_from")),
                  rs.getString("product_type"),
                  Db.orEmpty(rs, "goods_desc"),
                  Db.orEmpty(rs, "message_to_bank"),
                  Db.orEmpty(rs, "additional_info"));
            })
        .optional();
  }

  @Override
  public void saveRepay(UUID eventId, RepaymentRequest r) {
    jdbc.sql(
            "INSERT INTO fsa_repay_det (event_id, repayment_ref, repayment_action, value_date,"
                + " repayment_payer, currency_code, principal_amount, interest_amount, total_amount)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")
        .params(
            id(eventId), r.repaymentReference(), r.action().name(), r.valueDate(), r.payer().name(),
            r.principal().currencyCode(), r.principal().amount(), r.interest(), r.total())
        .update();
  }

  @Override
  public Optional<RepaymentRequest> repay(UUID eventId) {
    return jdbc.sql("SELECT * FROM fsa_repay_det WHERE event_id = ?")
        .param(id(eventId))
        .query(
            (rs, n) ->
                new RepaymentRequest(
                    rs.getString("repayment_ref"),
                    RepaymentAction.valueOf(rs.getString("repayment_action")),
                    rs.getObject("value_date", LocalDate.class),
                    RepaymentPayer.valueOf(rs.getString("repayment_payer")),
                    new Money(
                        rs.getBigDecimal("principal_amount"),
                        Currency.getInstance(rs.getString("currency_code").trim())),
                    rs.getBigDecimal("interest_amount")))
        .optional();
  }
}
