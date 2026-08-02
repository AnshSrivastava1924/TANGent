package com.tangent.service;

import com.tangent.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.tangent.constant.ApplicationConstants.ALPHA_VANTAGE_API_BASE;
import static com.tangent.constant.ApplicationConstants.ALPHA_VANTAGE_PROVIDER;
import static com.tangent.constant.ApplicationConstants.DEFAULT_SYMBOL;
import static com.tangent.constant.ApplicationConstants.MASSIVE_API_BASE;
import static com.tangent.constant.ApplicationConstants.MASSIVE_PROVIDER;
import static com.tangent.constant.ApplicationConstants.SYMBOL_PATTERN;

@Service
public class MarketDataService {

    private static final Logger log = LoggerFactory.getLogger(MarketDataService.class);

    private final String massiveKey;
    private final String alphaKey;
    private final String alphaEntitlement;
    private final RestClient http;
    private final ObjectMapper json;
    private final Object snapshotEntitlementLock = new Object();
    private final ConcurrentMap<String, CachedResponse> responseCache = new ConcurrentHashMap<>();
    private volatile boolean massiveSnapshotUnavailable;
    private volatile LocalDate groupedQuoteDate;
    private volatile Map<String, Map<String, Object>> groupedQuotes = Map.of();

    public MarketDataService(@Value("${market.massive.api-key:}") String massiveKey,
                             @Value("${market.alpha-vantage.api-key:}") String alphaKey,
                             @Value("${market.alpha-vantage.entitlement:}") String alphaEntitlement,
                             ObjectMapper json) {
        this.massiveKey = massiveKey;
        this.alphaKey = alphaKey;
        this.alphaEntitlement = alphaEntitlement == null ? "" : alphaEntitlement.trim().toLowerCase(Locale.ROOT);
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
        String cacheKey = "quote:" + symbol;
        Map<String, Object> cached = fresh(cacheKey, Duration.ofMinutes(15));
        if (cached != null) return cached;
        List<String> failures = new ArrayList<>();
        if (!massiveKey.isBlank()) {
            synchronized (snapshotEntitlementLock) {
                if (!massiveSnapshotUnavailable) {
                    try {
                        Optional<Map<String, Object>> value = massiveQuote(symbol);
                        if (value.isPresent()) return cache(cacheKey, value.get());
                        failures.add("Massive returned no snapshot");
                    } catch (RuntimeException exception) {
                        if (exception.getMessage() != null && exception.getMessage().contains("HTTP 403")) {
                            massiveSnapshotUnavailable = true;
                            log.info("Massive snapshot entitlement is unavailable; using official daily aggregates");
                        }
                        failures.add(providerFailure(MASSIVE_PROVIDER, exception));
                    }
                }
            }
            try {
                Optional<Map<String, Object>> value = massiveLatestBarQuote(symbol);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Massive returned no daily bars");
            } catch (RuntimeException fallbackException) {
                failures.add(providerFailure(MASSIVE_PROVIDER + " daily bars", fallbackException));
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = alphaQuote(symbol);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Alpha Vantage returned no quote");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(ALPHA_VANTAGE_PROVIDER, exception));
            }
        }
        Map<String, Object> stale = stale(cacheKey);
        if (stale != null) return stale;
        throw unavailable(symbol, failures);
    }

    public boolean isConfigured() {
        return !massiveKey.isBlank() || !alphaKey.isBlank();
    }

    public void validateSymbol(String requestedSymbol) {
        Map<String, Object> value = quote(requestedSymbol);
        Object price = value.get("price");
        if (!(price instanceof Number number) || number.doubleValue() <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ticker symbol was not found by the market provider");
        }
    }

    public Map<String, Object> history(String requestedSymbol, String range) {
        String symbol = symbol(requestedSymbol);
        String cacheKey = "history:" + symbol + ":" + normalizedRange(range);
        Map<String, Object> cached = fresh(cacheKey, Duration.ofHours(6));
        if (cached != null) return cached;
        List<String> failures = new ArrayList<>();
        if (!massiveKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = massiveHistory(symbol, range);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Massive returned no history");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(MASSIVE_PROVIDER, exception));
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = alphaHistory(symbol, range);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Alpha Vantage returned no history");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(ALPHA_VANTAGE_PROVIDER, exception));
            }
        }
        Map<String, Object> stale = stale(cacheKey);
        if (stale != null) return stale;
        throw unavailable(symbol, failures);
    }

    public Map<String, Object> compare(String symbols, String range) {
        List<Map<String, Object>> series = new ArrayList<>();
        for (String requested : symbols.split(",")) {
            Map<String, Object> history = history(requested, range);
            series.add(Map.of("symbol", symbol(requested), "provider", history.get("provider"),
                    "freshness", history.get("freshness"), "bars", history.get("bars")));
        }
        return Map.of("series", series, "generatedAt", java.time.Instant.now().toString());
    }

    public Map<String, Object> news(String symbol) {
        String cacheKey = "news:" + (symbol == null ? "" : symbol.toUpperCase(Locale.ROOT));
        Map<String, Object> cached = fresh(cacheKey, Duration.ofMinutes(30));
        if (cached != null) return cached;
        List<String> failures = new ArrayList<>();
        if (!massiveKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = massiveNews(symbol);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Massive returned no news");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(MASSIVE_PROVIDER, exception));
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = alphaNews(symbol);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Alpha Vantage returned no news");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(ALPHA_VANTAGE_PROVIDER, exception));
            }
        }
        Map<String, Object> stale = stale(cacheKey);
        if (stale != null) return stale;
        throw unavailable(symbol == null || symbol.isBlank() ? "market news" : symbol(symbol), failures);
    }

    public Map<String, Object> search(String query) {
        String cacheKey = "search:" + query.trim().toUpperCase(Locale.ROOT);
        Map<String, Object> cached = fresh(cacheKey, Duration.ofHours(24));
        if (cached != null) return cached;
        List<String> failures = new ArrayList<>();
        if (!massiveKey.isBlank()) {
            try {
                Optional<Map<String, Object>> value = massiveSearch(query);
                if (value.isPresent()) return cache(cacheKey, value.get());
                failures.add("Massive returned no matching symbols");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(MASSIVE_PROVIDER, exception));
            }
        }
        if (!alphaKey.isBlank()) {
            try {
                JsonNode root = fetch(ALPHA_VANTAGE_API_BASE + "?function=SYMBOL_SEARCH&keywords="
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
                if (!results.isEmpty()) return cache(cacheKey,
                        Map.of("provider", ALPHA_VANTAGE_PROVIDER, "results", results));
                failures.add("Alpha Vantage returned no matching symbols");
            } catch (RuntimeException exception) {
                failures.add(providerFailure(ALPHA_VANTAGE_PROVIDER, exception));
            }
        }
        Map<String, Object> stale = stale(cacheKey);
        if (stale != null) return stale;
        throw unavailable(query, failures);
    }

    private Optional<Map<String, Object>> massiveQuote(String symbol) {
        JsonNode root = fetch(MASSIVE_API_BASE + "/v2/snapshot/locale/us/markets/stocks/tickers/"
                + encode(symbol) + "?apiKey=" + encode(massiveKey));
        JsonNode ticker = root.path("ticker");
        if (ticker.isMissingNode()) return Optional.empty();
        JsonNode day = ticker.path("day");
        JsonNode previous = ticker.path("prevDay");
        double price = ticker.path("lastTrade").path("p").asDouble(day.path("c").asDouble());
        double previousClose = previous.path("c").asDouble(price);
        return Optional.of(quoteMap(MASSIVE_PROVIDER, "REALTIME_OR_DELAYED", symbol, price,
                day.path("o").asDouble(price), day.path("h").asDouble(price),
                day.path("l").asDouble(price), day.path("c").asDouble(price),
                day.path("v").asDouble(), previousClose));
    }

    private synchronized Optional<Map<String, Object>> massiveLatestBarQuote(String symbol) {
        LocalDate targetDate = latestWeekday(LocalDate.now().minusDays(1));
        if (!targetDate.equals(groupedQuoteDate) || groupedQuotes.isEmpty()) {
            JsonNode results = fetch(MASSIVE_API_BASE + "/v2/aggs/grouped/locale/us/market/stocks/"
                    + targetDate + "?adjusted=true&apiKey=" + encode(massiveKey)).path("results");
            if (!results.isArray() || results.isEmpty()) return Optional.empty();
            Map<String, Map<String, Object>> quotes = new ConcurrentHashMap<>();
            for (JsonNode bar : results) {
                String ticker = bar.path("T").asText();
                double price = bar.path("c").asDouble();
                Map<String, Object> quote = quoteMap(MASSIVE_PROVIDER, "END_OF_DAY", ticker, price,
                        bar.path("o").asDouble(price), bar.path("h").asDouble(price),
                        bar.path("l").asDouble(price), price, bar.path("v").asDouble(), price);
                quote.put("asOf", bar.path("t").asLong() > 0
                        ? Instant.ofEpochMilli(bar.path("t").asLong()).toString()
                        : targetDate.toString());
                quotes.put(ticker, Map.copyOf(quote));
            }
            groupedQuotes = Map.copyOf(quotes);
            groupedQuoteDate = targetDate;
            log.info("Cached {} Massive daily quotes from one grouped request", groupedQuotes.size());
        }
        return Optional.ofNullable(groupedQuotes.get(symbol));
    }

    private Optional<Map<String, Object>> alphaQuote(String symbol) {
        String entitlement = alphaEntitlement.isBlank() ? "" : "&entitlement=" + encode(alphaEntitlement);
        JsonNode quote = fetch(ALPHA_VANTAGE_API_BASE + "?function=GLOBAL_QUOTE&symbol="
                + encode(symbol) + entitlement + "&apikey=" + encode(alphaKey)).path("Global Quote");
        double price = decimal(quote, "05. price");
        if (price == 0) return Optional.empty();
        double previous = decimal(quote, "08. previous close");
        String freshness = "realtime".equals(alphaEntitlement) ? "REALTIME"
                : "delayed".equals(alphaEntitlement) ? "DELAYED" : "END_OF_DAY";
        return Optional.of(quoteMap(ALPHA_VANTAGE_PROVIDER, freshness, symbol, price,
                decimal(quote, "02. open"), decimal(quote, "03. high"),
                decimal(quote, "04. low"), price, decimal(quote, "06. volume"), previous));
    }

    private Optional<Map<String, Object>> massiveHistory(String symbol, String range) {
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusDays(days(range));
        JsonNode results = fetch(MASSIVE_API_BASE + "/v2/aggs/ticker/" + encode(symbol)
                + "/range/1/day/" + from + "/" + to
                + "?adjusted=true&sort=asc&limit=5000&apiKey=" + encode(massiveKey)).path("results");
        if (!results.isArray() || results.isEmpty()) return Optional.empty();
        List<Map<String, Object>> bars = new ArrayList<>();
        for (JsonNode bar : results) {
            bars.add(bar(bar.path("t").asLong(), null, bar.path("o").asDouble(),
                    bar.path("h").asDouble(), bar.path("l").asDouble(),
                    bar.path("c").asDouble(), bar.path("v").asDouble()));
        }
        return Optional.of(Map.of("provider", MASSIVE_PROVIDER, "freshness", "END_OF_DAY",
                "symbol", symbol, "bars", bars));
    }

    private Optional<Map<String, Object>> alphaHistory(String symbol, String range) {
        JsonNode series = fetch(ALPHA_VANTAGE_API_BASE + "?function=TIME_SERIES_DAILY&symbol="
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
        return Optional.of(Map.of("provider", ALPHA_VANTAGE_PROVIDER, "freshness", "END_OF_DAY",
                "symbol", symbol, "bars", bars));
    }

    private Optional<Map<String, Object>> massiveNews(String requestedSymbol) {
        String ticker = requestedSymbol == null || requestedSymbol.isBlank() ? "" : "ticker=" + encode(symbol(requestedSymbol)) + "&";
        JsonNode results = fetch(MASSIVE_API_BASE + "/v2/reference/news?" + ticker
                + "limit=12&order=desc&sort=published_utc&apiKey=" + encode(massiveKey)).path("results");
        if (!results.isArray() || results.isEmpty()) return Optional.empty();
        List<Map<String, Object>> articles = new ArrayList<>();
        for (JsonNode item : results) {
            articles.add(article(item.path("title").asText(), item.path("description").asText(),
                    item.path("article_url").asText(), item.path("published_utc").asText(),
                    item.path("publisher").path("name").asText("Market news")));
        }
        return Optional.of(Map.of("provider", MASSIVE_PROVIDER, "articles", articles));
    }

    private Optional<Map<String, Object>> alphaNews(String requestedSymbol) {
        String scope = requestedSymbol == null || requestedSymbol.isBlank()
                ? "topics=financial_markets" : "tickers=" + encode(symbol(requestedSymbol));
        JsonNode feed = fetch(ALPHA_VANTAGE_API_BASE + "?function=NEWS_SENTIMENT&" + scope
                + "&limit=12&apikey=" + encode(alphaKey)).path("feed");
        if (!feed.isArray() || feed.isEmpty()) return Optional.empty();
        List<Map<String, Object>> articles = new ArrayList<>();
        for (JsonNode item : feed) {
            articles.add(article(item.path("title").asText(), item.path("summary").asText(),
                    item.path("url").asText(), item.path("time_published").asText(),
                    item.path("source").asText("Market news")));
        }
        return Optional.of(Map.of("provider", ALPHA_VANTAGE_PROVIDER, "articles", articles));
    }

    private Optional<Map<String, Object>> massiveSearch(String query) {
        JsonNode results = fetch(MASSIVE_API_BASE + "/v3/reference/tickers?search=" + encode(query)
                + "&market=stocks&active=true&limit=8&apiKey=" + encode(massiveKey)).path("results");
        if (!results.isArray() || results.isEmpty()) return Optional.empty();
        List<Map<String, Object>> matches = new ArrayList<>();
        for (JsonNode item : results) {
            matches.add(Map.of(
                    "symbol", item.path("ticker").asText(),
                    "name", item.path("name").asText(),
                    "region", item.path("locale").asText("us").toUpperCase(Locale.ROOT)
            ));
        }
        return Optional.of(Map.of("provider", MASSIVE_PROVIDER, "results", matches));
    }

    private JsonNode fetch(String url) {
        try {
            String body = http.get().uri(url).retrieve().body(String.class);
            JsonNode root = json.readTree(body);
            String providerMessage = providerMessage(root);
            if (providerMessage != null) throw new IllegalStateException(providerMessage);
            return root;
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException("HTTP " + exception.getStatusCode().value(), exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException stateException) throw stateException;
            throw new IllegalStateException("Market provider request failed", exception);
        }
    }

    private Map<String, Object> quoteMap(String provider, String freshness, String symbol, double price, double open,
                                         double high, double low, double close, double volume, double previous) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("provider", provider);
        value.put("freshness", freshness);
        value.put("asOf", java.time.Instant.now().toString());
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

    private String providerMessage(JsonNode root) {
        for (String field : List.of("Error Message", "Information", "Note", "error")) {
            String message = root.path(field).asText("").trim();
            if (!message.isEmpty()) return sanitize(message);
        }
        return null;
    }

    private String providerFailure(String provider, RuntimeException exception) {
        String message = sanitize(exception.getMessage());
        log.warn("{} market request failed: {}", provider, message);
        String lower = message.toLowerCase(Locale.ROOT);
        if (lower.contains("429") || lower.contains("rate limit")) {
            return provider + ": request limit reached; retry later";
        }
        if (lower.contains("403")) {
            return provider + ": current plan does not allow this endpoint";
        }
        return provider + ": provider request failed";
    }

    private ApiException unavailable(String subject, List<String> failures) {
        String detail = failures.isEmpty()
                ? "No market-data API key is configured"
                : String.join("; ", failures);
        return new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                "Live market data is unavailable for " + subject + ". " + detail);
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) return "provider request failed";
        String sanitized = message.replaceAll("(?i)(apiKey=)[^&\\s]+", "$1***");
        if (!massiveKey.isBlank()) sanitized = sanitized.replace(massiveKey, "***");
        if (!alphaKey.isBlank()) sanitized = sanitized.replace(alphaKey, "***");
        return sanitized.length() > 180 ? sanitized.substring(0, 180) + "..." : sanitized;
    }

    private Map<String, Object> cache(String key, Map<String, Object> value) {
        Map<String, Object> immutable = Map.copyOf(value);
        responseCache.put(key, new CachedResponse(immutable, Instant.now()));
        return immutable;
    }

    private Map<String, Object> fresh(String key, Duration ttl) {
        CachedResponse cached = responseCache.get(key);
        if (cached == null || cached.cachedAt().plus(ttl).isBefore(Instant.now())) return null;
        return cached.value();
    }

    private Map<String, Object> stale(String key) {
        CachedResponse cached = responseCache.get(key);
        if (cached == null) return null;
        Map<String, Object> value = new LinkedHashMap<>(cached.value());
        value.put("freshness", "STALE_CACHE");
        value.put("cachedAt", cached.cachedAt().toString());
        return Map.copyOf(value);
    }

    private LocalDate latestWeekday(LocalDate date) {
        LocalDate result = date;
        while (result.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                || result.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            result = result.minusDays(1);
        }
        return result;
    }

    private String normalizedRange(String range) {
        return switch (range == null ? "3mo" : range) {
            case "1mo", "3mo", "6mo", "1y" -> range == null ? "3mo" : range;
            default -> "3mo";
        };
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
        String symbol = value == null ? DEFAULT_SYMBOL : value.trim().toUpperCase(Locale.ROOT);
        return symbol.matches(SYMBOL_PATTERN) ? symbol : DEFAULT_SYMBOL;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private record CachedResponse(Map<String, Object> value, Instant cachedAt) {
    }
}
