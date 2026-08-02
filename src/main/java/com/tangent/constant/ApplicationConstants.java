package com.tangent.constant;

import java.util.List;

public final class ApplicationConstants {

    public static final String DEFAULT_SYMBOL = "AAPL";
    public static final String DEFAULT_RANGE = "3mo";
    public static final String MASSIVE_PROVIDER = "Massive";
    public static final String ALPHA_VANTAGE_PROVIDER = "Alpha Vantage";
    public static final String MASSIVE_API_BASE = "https://api.massive.com";
    public static final String ALPHA_VANTAGE_API_BASE = "https://www.alphavantage.co/query";
    public static final String SYMBOL_PATTERN = "[A-Z0-9.-]{1,15}";
    public static final List<String> EXPENSE_CATEGORIES = List.of(
            "Food", "Health", "Housing", "Utilities", "Transport", "Family", "Leisure");

    private ApplicationConstants() {
    }
}
