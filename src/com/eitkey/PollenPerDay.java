package com.eitkey;

import java.time.LocalDate;

public class PollenPerDay {
    private final LocalDate date;
    private final int amount;
    private final boolean counted;

    public PollenPerDay(LocalDate date, int amount, boolean counted) {
        this.date = date;
        this.amount = amount;
        this.counted = counted;
    }

    public int getAmount() {
        return amount;
    }

    public LocalDate getDate() {
        return date;
    }

    public boolean isCounted() {
        return counted;
    }
}