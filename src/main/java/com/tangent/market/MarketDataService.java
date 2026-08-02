package com.tangent.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Duration;
import java.time.ZoneOffset;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Service
public class MarketDataService {

    private final String massiveKey;
    private final String alphaKey;
    private final RestClient http;
    private final ObjectMapper json;

    public MarketDataService(@Value("${market.massive.api-key:}") String massiveKey,
                             @Value("${market.alpha-vantage.api-key:}") String alphaKey,
                             ObjectMapper json) {
        this.massiveKey = massiveKey;
        this.alphaKey = alphaKey;
        this.json = json;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(client);
        requests.setReadTimeout(Duration.ofSeconds(6));
        this.http = RestClient.builder().requestFactory(requests).build();
    }

    public Map<String, Object> quote(String requestedSymbol) {
        String symbol = symbol(requestedSymbol);
        if (!massiveKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = massiveQuote(symbol);
                if (value.isPresent()) return value.get();
            } catch (RuntimeException ignored) {
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = alphaQuote(symbol);
                if (value.isPresent()) return value.get();
            } catch (RuntimeException ignored) {
            }
        }
        return demoQuote(symbol);
    }

    public Map<String, Object> history(String requestedSymbol, String range) {
        String symbol = symbol(requestedSymbol);
        if (!massiveKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = massiveHistory(symbol, range);
                if (value.isPresent()) return value.get();
            } catch (RuntimeException ignored) {
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = alphaHistory(symbol, range);
                if (value.isPresent()) return value.get();
            } catch (RuntimeException ignored) {
            }
        }
        return demoHistory(symbol, range);
    }

    public Map<String, Object> compare(String symbols, String range) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (String requested : symbols.split(",")) {
            Map<String, Object> history = history(requested, range);
            series.add(Map.of("symbol", symbol(requested), "bars", history.get("bars")));
        }
        return Map.of("series", series);
    }

    public Map<String, Object> news(String symbol) {
        if (!massiveKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = massiveNews(symbol);
                if (value.isPresent()) return value.get();
            } catch (RuntimeException ignored) {
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = alphaNews(symbol);
                if (value.isPresent()) return value.get();
            } catch (RuntimeException ignored) {
            }
        }
        return demoNews(symbol);
    }

    public Map<String, Object> search(String query) {
        if (!alphaKey.isBlank()) {
            try {
                JsonNode root = fetch("https://www.alphavantage.co/query?function=SYMBOL_SEARCH&keywords="
                        + encode(query) + "&apikey=" + encode(alphaKey));
                List<Map<String, Object>> results = new ArrayList<>();
                for (JsonNode item : root.path("bestMatches")) {
                    results.add(Map.of(
                            "symbol", item.path("1. symbol").asText(),
                            "name", item.path("2. name").asText(),
                            "region", item.path("4. region").asText()
                    ));
                    if (results.size() == 8) break;
                }
                if (!results.isEmpty()) return Map.of("provider", "Alpha Vantage", "results", results);
            } catch (RuntimeException ignored) {
            }
        }
        return Map.of("provider", "Demo fallback", "results", List.of(
                Map.of("symbol", "AAPL", "name", "Apple Inc.", "region", "United States"),
                Map.of("symbol", "MSFT", "name", "Microsoft Corporation", "region", "United States"),
                Map.of("symbol", "NVDA", "name", "NVIDIA Corporation", "region", "United States")
        ));
    }

    private Optional<Map<String, Object>> massiveQuote(String symbol) {
        JsonNode root = fetch("https://api.massive.com/v2/snapshot/locale/us/markets/stocks/tickers/"
                + encode(symbol) + "?apiKey=" + encode(massiveKey));
        JsonNode ticker = root.path("ticker");
        if (ticker.isMissingNode()) return Optional.empty();
        JsonNode day = ticker.path("day");
        JsonNode previous = ticker.path("prevDay");
        double price = ticker.path("lastTrade").path("p").asDouble(day.path("c").asDouble());
        double previousClose = previous.path("c").asDouble(price);
        return Optional.of(quoteMap("Massive", symbol, price,
                day.path("o").asDouble(price), day.path("h").asDouble(price),
                day.path("l").asDouble(price), day.path("c").asDouble(price),
                day.path("v").asDouble(), previousClose));
    }

    private Optional<Map<String, Object>> alphaQuote(String symbol) {
        JsonNode quote = fetch("https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol="
                + encode(symbol) + "&apikey=" + encode(alphaKey)).path("Global Quote");
        double price = decimal(quote, "05. price");
        if (price == 0) return Optional.empty();
        double previous = decimal(quote, "08. previous close");
        return Optional.of(quoteMap("Alpha Vantage", symbol, price,
                decimal(quote, "02. open"), decimal(quote, "03. high"),
                decimal(quote, "04. low"), price, decimal(quote, "06. volume"), previous));
    }

    private Optional<Map<String, Object>> massiveHistory(String symbol, String range) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days(range));
        JsonNode results = fetch("https://api.massive.com/v2/aggs/ticker/" + encode(symbol)
                + "/range/1/day/" + from + "/" + to
                + "?adjusted=true&sort=asc&limit=5000&apiKey=" + encode(massiveKey)).path("results");
        if (!results.isArray() || results.isEmpty()) return Optional.empty();
        List<Map<String, Object>> bars = new ArrayList<>();
        for (JsonNode bar : results) {
            bars.add(bar(bar.path("t").asLong(), null, bar.path("o").asDouble(),
                    bar.path("h").asDouble(), bar.path("l").asDouble(),
                    bar.path("c").asDouble(), bar.path("v").asDouble()));
        }
        return Optional.of(Map.of("provider", "Massive", "symbol", symbol, "bars", bars));
    }

    private Optional<Map<String, Object>> alphaHistory(String symbol, String range) {
        JsonNode series = fetch("https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="
                + encode(symbol) + "&outputsize=compact&apikey=" + encode(alphaKey)).path("Time Series (Daily)");
        if (!series.isObject() || series.isEmpty()) return Optional.empty();
        List<String> dates = new ArrayList<>();
        dates.addAll(series.propertyNames());
        dates.sort(String::compareTo);
        int start = Math.max(0, dates.size() - days(range));
        List<Map<String, Object>> bars = new ArrayList<>();
        for (String date : dates.subList(start, dates.size())) {
            JsonNode item = series.path(date);
            long time = LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
            bars.add(bar(time, date, decimal(item, "1. open"), decimal(item, "2. high"),
                    decimal(item, "3. low"), decimal(item, "4. close"), decimal(item, "5. volume")));
        }
        return Optional.of(Map.of("provider", "Alpha Vantage", "symbol", symbol, "bars", bars));
    }

    private Optional<Map<String, Object>> massiveNews(String requestedSymbol) {
        String ticker = requestedSymbol == null || requestedSymbol.isBlank() ? "" : "ticker=" + encode(symbol(requestedSymbol)) + "&";
        JsonNode results = fetch("https://api.massive.com/v2/reference/news?" + ticker
                + "limit=12&order=desc&sort=published_utc&apiKey=" + encode(massiveKey)).path("results");
        if (!results.isArray() || results.isEmpty()) return Optional.empty();
        List<Map<String, Object>> articles = new ArrayList<>();
        for (JsonNode item : results) {
            articles.add(article(item.path("title").asText(), item.path("description").asText(),
                    item.path("article_url").asText(), item.path("published_utc").asText(),
                    item.path("publisher").path("name").asText("Market news")));
        }
        return Optional.of(Map.of("provider", "Massive", "articles", articles));
    }

    private Optional<Map<String, Object>> alphaNews(String requestedSymbol) {
        String scope = requestedSymbol == null || requestedSymbol.isBlank()
                ? "topics=financial_markets" : "tickers=" + encode(symbol(requestedSymbol));
        JsonNode feed = fetch("https://www.alphavantage.co/query?function=NEWS_SENTIMENT&" + scope
                + "&limit=12&apikey=" + encode(alphaKey)).path("feed");
        if (!feed.isArray() || feed.isEmpty()) return Optional.empty();
        List<Map<String, Object>> articles = new ArrayList<>();
        for (JsonNode item : feed) {
            articles.add(article(item.path("title").asText(), item.path("summary").asText(),
                    item.path("url").asText(), item.path("time_published").asText(),
                    item.path("source").asText("Market news")));
        }
        return Optional.of(Map.of("provider", "Alpha Vantage", "articles", articles));
    }

    private JsonNode fetch(String url) {
        try {
            String body = http.get().uri(url).retrieve().body(String.class);
            return json.readTree(body);
        } catch (Exception exception) {
            throw new IllegalStateException("Market provider request failed", exception);
        }
    }

    private Map<String, Object> demoQuote(String symbol) {
        double price = 145 + Math.abs(symbol.hashCode() % 120);
        return quoteMap("Demo fallback", symbol, price, price - 1.2, price + 3.4,
                price - 4.1, price, 34_500_000, price - 2.8);
    }

    private Map<String, Object> demoHistory(String symbol, String range) {
        List<Map<String, Object>> bars = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(Math.min(days(range), 100));
        double base = 120 + Math.abs(symbol.hashCode() % 90);
        int count = Math.min(days(range), 100);
        for (int i = 0; i < count; i++) {
            LocalDate date = start.plusDays(i);
            double close = base + Math.sin(i / 5.0) * 4 + Math.cos(i / 11.0) * 8 + i * 0.18;
            bars.add(bar(date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(), date.toString(),
                    close - 1.5, close + 2.4, close - 2.7, close, 28_000_000 + i * 110_000L));
        }
        return Map.of("provider", "Demo fallback", "symbol", symbol, "bars", bars);
    }

    private Map<String, Object> demoNews(String requestedSymbol) {
        String subject = requestedSymbol == null || requestedSymbol.isBlank() ? "Market" : symbol(requestedSymbol);
        return Map.of("provider", "Demo fallback", "articles", List.of(
                article(subject + " briefing: growth, rates and valuation in focus",
                        "Configure MASSIVE_API_KEY or ALPHA_VANTAGE_API_KEY to enable live articles.",
                        "https://www.alphavantage.co/documentation/", "Today", "TANGent demo"),
                article("Analysts compare mega-cap momentum as earnings season approaches",
                        "This fallback keeps the application usable before live provider keys are configured.",
                        "https://massive.com/docs/rest", "Today", "TANGent demo")
        ));
    }

    private Map<String, Object> quoteMap(String provider, String symbol, double price, double open,
                                         double high, double low, double close, double volume, double previous) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("provider", provider);
        value.put("symbol", symbol);
        value.put("price", price);
        value.put("open", open);
        value.put("high", high);
        value.put("low", low);
        value.put("close", close);
        value.put("volume", volume);
        value.put("change", price - previous);
        value.put("changePercent", previous == 0 ? 0 : (price - previous) / previous * 100);
        return value;
    }

    private Map<String, Object> bar(long time, String date, double open, double high,
                                    double low, double close, double volume) {
        Map<String, Object> value = new LinkedHashMap<>();
        if (date != null) value.put("date", date);
        value.put("time", time);
        value.put("open", open);
        value.put("high", high);
        value.put("low", low);
        value.put("close", close);
        value.put("volume", volume);
        return value;
    }

    private Map<String, Object> article(String title, String summary, String url, String published, String source) {
        return Map.of("title", title, "summary", summary, "url", url,
                "publishedAt", published, "source", source);
    }

    private double decimal(JsonNode node, String field) {
        try {
            return Double.parseDouble(node.path(field).asText("0").replace("%", "").replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private int days(String range) {
        return switch (range == null ? "3mo" : range) {
            case "1mo" -> 32;
            case "6mo" -> 190;
            case "1y" -> 370;
            default -> 96;
        };
    }

    private String symbol(String value) {
        String symbol = value == null ? "AAPL" : value.trim().toUpperCase(Locale.ROOT);
        return symbol.matches("[A-Z0-9.-]{1,15}") ? symbol : "AAPL";
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
