package com.danlind.igz.domain;

import java.util.Objects;

/**
 * Created by danlin on 2017-03-27.
 */
public class AccountDetails {

    private final double balance;
    private final double profitLoss;
    private final double marginValue;


    public AccountDetails(double balance, double profitLoss, double marginValue) {
        this.profitLoss = profitLoss;
        this.balance = balance;
        this.marginValue = marginValue;
    }

    public double getMarginValue() {
        return marginValue;
    }

    public double getProfitLoss() {
        return profitLoss;
    }

    public double getBalance() {
        return balance;
    }
}
