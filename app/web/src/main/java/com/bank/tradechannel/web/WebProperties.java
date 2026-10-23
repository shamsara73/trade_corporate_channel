package com.bank.tradechannel.web;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code channel.web}.
 *
 * @param environmentLabel shown in the page header so testers never mistake a test system for
 *     production, for example "Development" or "UAT"
 */
@ConfigurationProperties("channel.web")
public record WebProperties(String environmentLabel) {

  public WebProperties {
    environmentLabel = environmentLabel == null ? "" : environmentLabel;
  }
}
