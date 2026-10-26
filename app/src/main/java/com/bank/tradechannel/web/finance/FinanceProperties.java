package com.bank.tradechannel.web.finance;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Choices offered on the finance page, under {@code channel.finance}.
 *
 * @param currencies ISO codes the corporate may finance in
 * @param productTypes TI product types for standalone finance (PSF in the bank's test)
 * @param defaultTenorDays pre-filled tenor (60 in the bank's test)
 */
@ConfigurationProperties("channel.finance")
public record FinanceProperties(
    List<String> currencies, List<String> productTypes, int defaultTenorDays) {}
