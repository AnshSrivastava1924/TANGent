package com.tangent.market;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketDataService market;

    public MarketController(MarketDataService market) {
        this.market = market;
    }

    @GetMapping("/quote/{symbol}")
    @Operation(summary = "Get a normalized stock quote")
    public Map<String, Object> quote(@PathVariable String symbol) {
        return market.quote(symbol);
    }

    @GetMapping("/history/{symbol}")
    @Operation(summary = "Get normalized OHLCV history")
    public Map<String, Object> history(@PathVariable String symbol,
                                       @RequestParam(defaultValue = "3mo") String range) {
        return market.history(symbol, range);
    }

    @GetMapping("/compare")
    @Operation(summary = "Get historical bars for multiple symbols")
    public Map<String, Object> compare(@RequestParam(defaultValue = "AAPL,MSFT,NVDA") String symbols,
                                       @RequestParam(defaultValue = "3mo") String range) {
        return market.compare(symbols, range);
    }

    @GetMapping("/news")
    @Operation(summary = "Get market news")
    public Map<String, Object> news(@RequestParam(defaultValue = "") String symbol) {
        return market.news(symbol);
    }

    @GetMapping("/search")
    @Operation(summary = "Search stock symbols")
    public Map<String, Object> search(@RequestParam(name = "q", defaultValue = "AAPL") String query) {
        return market.search(query);
    }
}
