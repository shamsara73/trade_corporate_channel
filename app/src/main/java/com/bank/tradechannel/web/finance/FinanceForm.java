package com.bank.tradechannel.web.finance;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * What the finance request page posts. Lengths and code lists follow TI's XSD for TFFINNEW; the
 * customer is not here on purpose (ground rule 8).
 */
public class FinanceForm {
  static final String AMOUNT = "\\d{1,15}(\\.\\d{1,3})?";

  @NotBlank
  @Size(max = 34)
  @Pattern(regexp = "[\\p{Print}&&[^<>]]*", message = "{finance.error.characters}")
  private String theirReference;

  @NotBlank
  @Pattern(regexp = AMOUNT, message = "{finance.error.amount}")
  private String financeAmount;

  @Pattern(regexp = "|" + AMOUNT, message = "{finance.error.amount}")
  private String transactionAmount = "";

  @NotBlank
  @Pattern(regexp = "[A-Z]{3}")
  private String currency;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
  private LocalDate startDate;

  @NotNull
  @Min(1)
  @Max(3650)
  private Integer tenorDays;

  private boolean trustReceiptLoan;

  @NotBlank
  @Pattern(regexp = "AFTER|FROM|FROM_EXCLUSIVE")
  private String interestFrom;

  @NotBlank
  @Pattern(regexp = "[A-Z0-9]{1,3}")
  private String productType;

  @Size(max = 50)
  private String goodsDescription = "";

  @Size(max = 2000)
  private String messageToBank = "";

  @Size(max = 222)
  private String additionalInformation = "";

  public String getTheirReference() {
    return theirReference;
  }

  public void setTheirReference(String theirReference) {
    this.theirReference = theirReference;
  }

  public String getFinanceAmount() {
    return financeAmount;
  }

  public void setFinanceAmount(String financeAmount) {
    this.financeAmount = financeAmount;
  }

  public String getTransactionAmount() {
    return transactionAmount;
  }

  public void setTransactionAmount(String transactionAmount) {
    this.transactionAmount = transactionAmount;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDate startDate) {
    this.startDate = startDate;
  }

  public Integer getTenorDays() {
    return tenorDays;
  }

  public void setTenorDays(Integer tenorDays) {
    this.tenorDays = tenorDays;
  }

  public boolean isTrustReceiptLoan() {
    return trustReceiptLoan;
  }

  public void setTrustReceiptLoan(boolean trustReceiptLoan) {
    this.trustReceiptLoan = trustReceiptLoan;
  }

  public String getInterestFrom() {
    return interestFrom;
  }

  public void setInterestFrom(String interestFrom) {
    this.interestFrom = interestFrom;
  }

  public String getProductType() {
    return productType;
  }

  public void setProductType(String productType) {
    this.productType = productType;
  }

  public String getGoodsDescription() {
    return goodsDescription;
  }

  public void setGoodsDescription(String goodsDescription) {
    this.goodsDescription = goodsDescription;
  }

  public String getMessageToBank() {
    return messageToBank;
  }

  public void setMessageToBank(String messageToBank) {
    this.messageToBank = messageToBank;
  }

  public String getAdditionalInformation() {
    return additionalInformation;
  }

  public void setAdditionalInformation(String additionalInformation) {
    this.additionalInformation = additionalInformation;
  }
}
