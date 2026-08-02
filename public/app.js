const state = {
  user: null,
  config: null,
  symbol: "AAPL",
  watchSymbol: "AAPL",
  range: "3mo",
  majorStocks: ["AAPL", "MSFT", "NVDA", "GOOGL", "AMZN", "META", "TSLA", "JPM"],
  watchlist: ["AAPL", "MSFT", "NVDA"],
  portfolioClasses: [
    {
      id: "cash",
      name: "Cash and Bank Accounts",
      purpose: "Ready money for bills, emergencies, and near-term care needs.",
      items: [
        { name: "Checking account", value: 24500, income: 0, note: "Monthly spending account" },
        { name: "Savings account", value: 78000, income: 1800, note: "Emergency reserve" },
        { name: "Fixed deposit ladder", value: 135000, income: 6400, note: "Low-risk income" }
      ]
    },
    {
      id: "securities",
      name: "Listed Securities",
      purpose: "Stocks and ETFs that provide growth and dividend potential.",
      items: [
        { name: "Dividend stock basket", value: 162000, income: 6200, note: "Blue-chip shares" },
        { name: "Broad market ETF", value: 118000, income: 2100, note: "Diversified equity exposure" }
      ]
    },
    {
      id: "fixedIncome",
      name: "Bonds and Fixed Income",
      purpose: "Stability, predictable coupons, and lower volatility.",
      items: [
        { name: "Government bonds", value: 220000, income: 10500, note: "Core retirement income" },
        { name: "Municipal bond fund", value: 94000, income: 3900, note: "Tax-aware income" }
      ]
    },
    {
      id: "funds",
      name: "Mutual Funds and ETFs",
      purpose: "Managed diversification across markets and sectors.",
      items: [
        { name: "Balanced mutual fund", value: 86000, income: 2600, note: "Moderate risk" },
        { name: "Healthcare ETF", value: 42000, income: 700, note: "Sector allocation" }
      ]
    },
    {
      id: "pension",
      name: "Pension Sources",
      purpose: "Expected yearly income from retirement plans.",
      items: [
        { name: "Company pension", value: 0, income: 42000, note: "Annual pension income" },
        { name: "Social security", value: 0, income: 31800, note: "Annual benefit estimate" }
      ]
    },
    {
      id: "annuities",
      name: "Annuities",
      purpose: "Contracted income that can support regular expenses.",
      items: [
        { name: "Lifetime annuity", value: 175000, income: 15600, note: "Guaranteed yearly payout" }
      ]
    },
    {
      id: "housing",
      name: "Housing and Real Estate",
      purpose: "Home equity and rental property value.",
      items: [
        { name: "Primary home", value: 485000, income: 0, note: "Mortgage-free residence" },
        { name: "Rental apartment", value: 265000, income: 18000, note: "Rental income property" }
      ]
    },
    {
      id: "commodities",
      name: "Gold and Commodities",
      purpose: "Inflation hedge and alternative asset exposure.",
      items: [
        { name: "Gold holdings", value: 36000, income: 0, note: "Long-term reserve" }
      ]
    },
    {
      id: "insurance",
      name: "Insurance Cash Value",
      purpose: "Policies with accessible value or estate-planning support.",
      items: [
        { name: "Whole life cash value", value: 58000, income: 0, note: "Policy cash value" }
      ]
    },
    {
      id: "liabilities",
      name: "Loans and Debts",
      purpose: "Amounts owed that reduce household net worth.",
      isLiability: true,
      items: [
        { name: "Home equity line", value: 41000, income: 0, note: "Outstanding balance" },
        { name: "Car loan", value: 12500, income: 0, note: "Remaining balance" }
      ]
    }
  ],
  expenses: [
    { date: "2026-07-31", name: "Groceries", category: "Food", amount: 84 },
    { date: "2026-07-31", name: "Prescription refill", category: "Health", amount: 42 },
    { date: "2026-07-30", name: "Electric bill", category: "Utilities", amount: 126 },
    { date: "2026-07-30", name: "Taxi to clinic", category: "Transport", amount: 28 },
    { date: "2026-07-29", name: "Dinner with family", category: "Family", amount: 96 },
    { date: "2026-07-28", name: "Gardening supplies", category: "Housing", amount: 64 },
    { date: "2026-07-27", name: "Movie tickets", category: "Leisure", amount: 34 }
  ],
  charts: {}
};

const money = new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" });
const number = new Intl.NumberFormat("en-US", { maximumFractionDigits: 0 });
let authMode = "login";
const searchTimers = new Map();

document.addEventListener("DOMContentLoaded", () => {
  initializeTheme();
  bindAuth();
  bindShell();
  loadConfig();
  if (localStorage.getItem("tangent-session")) openDashboard();
});

function bindAuth() {
  document.querySelectorAll("[data-mode]").forEach((button) => {
    button.addEventListener("click", () => {
      document.querySelectorAll("[data-mode]").forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      authMode = button.dataset.mode;
      document.getElementById("fullNameLabel").classList.toggle("hidden", authMode !== "signup");
      document.getElementById("authNote").textContent =
        authMode === "signup" ? "Create a new database-backed TANGent account." : "Demo login: student@tangent.local / training123";
    });
  });

  document.getElementById("authForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const payload = {
      mode: authMode,
      email: form.get("email"),
      password: form.get("password"),
      fullName: form.get("fullName")
    };
    try {
      const data = await apiJson("/api/auth", { method: "POST", body: payload, authenticated: false });
      localStorage.setItem("tangent-session", data.token);
      document.getElementById("authNote").textContent = "Authentication successful.";
      await openDashboard();
    } catch (error) {
      document.getElementById("authNote").textContent = error.message;
    }
  });
}

function bindShell() {
  document.getElementById("homeButton").addEventListener("click", () => showPage("home"));
  document.getElementById("logoutButton").addEventListener("click", () => {
    localStorage.removeItem("tangent-session");
    document.getElementById("dashboard").classList.add("hidden");
    document.getElementById("authView").classList.remove("hidden");
  });
  document.getElementById("themeButton").addEventListener("click", toggleTheme);
  document.getElementById("refreshButton").addEventListener("click", async (event) => {
    const button = event.currentTarget;
    button.disabled = true;
    try {
      await refreshActivePage();
      showToast("Page refreshed", "success");
    } catch (error) {
      showToast(error.message, "error");
    } finally {
      button.disabled = false;
    }
  });

  document.querySelectorAll("[data-page]").forEach((card) => {
    card.addEventListener("click", () => showPage(card.dataset.page));
  });

  document.querySelectorAll("[data-nav-page]").forEach((button) => {
    button.addEventListener("click", () => showPage(button.dataset.navPage));
  });

  bindTickerSearch("symbolInput", "symbolSuggestions");
  bindTickerSearch("watchlistInput", "watchlistSuggestions");

  document.getElementById("symbolForm").addEventListener("submit", (event) => {
    event.preventDefault();
    state.symbol = cleanSymbol(document.getElementById("symbolInput").value, "AAPL");
    state.range = document.getElementById("rangeSelect").value;
    hideSuggestions("symbolInput", "symbolSuggestions");
    loadOverviewStock(state.symbol);
  });

  document.getElementById("compareForm").addEventListener("submit", (event) => {
    event.preventDefault();
    loadComparison();
  });

  document.getElementById("watchlistForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    const input = document.getElementById("watchlistInput");
    const status = document.getElementById("watchlistStatus");
    const button = form.querySelector('button[type="submit"]');
    const symbol = cleanSymbol(input.value, "");
    hideSuggestions("watchlistInput", "watchlistSuggestions");

    if (!symbol) {
      status.textContent = "Enter a valid ticker symbol.";
      input.focus();
      return;
    }
    if (state.watchlist.includes(symbol)) {
      status.textContent = `${symbol} is already in your watchlist.`;
      input.select();
      return;
    }

    button.disabled = true;
    status.textContent = `Saving ${symbol}...`;
    try {
      await apiJson("/api/app/watchlist", { method: "POST", body: { symbol } });
      state.watchlist.push(symbol);
      state.watchSymbol = symbol;
      input.value = "";
      status.textContent = `${symbol} saved. Loading market data...`;

      const [, chart] = await Promise.allSettled([
        renderWatchlist(),
        loadWatchlistChart(symbol)
      ]);
      status.textContent = chart.status === "fulfilled"
        ? `${symbol} was added to your watchlist.`
        : `${symbol} was saved; its chart is temporarily unavailable.`;
      showToast(`${symbol} added to your watchlist`, "success");
    } catch (error) {
      status.textContent = `Could not add ${symbol}: ${error.message}`;
      showToast(`Could not add ${symbol}`, "error");
    } finally {
      button.disabled = false;
    }
  });

  document.getElementById("expenseForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const name = document.getElementById("expenseName").value.trim() || "Expense";
    const category = document.getElementById("expenseCategory").value;
    const amount = finiteNumber(document.getElementById("expenseAmount").value);
    if (amount <= 0) return;
    const expense = await apiJson("/api/app/expenses", {
      method: "POST",
      body: { date: todayIso(), name, category, amount }
    });
    state.expenses.unshift(expense);
    document.getElementById("expenseName").value = "";
    document.getElementById("expenseAmount").value = "50";
    renderBuddy();
    showToast("Expense added", "success");
  });

  document.addEventListener("click", (event) => {
    if (!event.target.closest(".search-field")) {
      hideSuggestions("symbolInput", "symbolSuggestions");
      hideSuggestions("watchlistInput", "watchlistSuggestions");
    }
  });
}

async function openDashboard() {
  try {
    await loadAppData();
    document.getElementById("authView").classList.add("hidden");
    document.getElementById("dashboard").classList.remove("hidden");
    showPage("home");
  } catch (error) {
    localStorage.removeItem("tangent-session");
    document.getElementById("dashboard").classList.add("hidden");
    document.getElementById("authView").classList.remove("hidden");
    document.getElementById("authNote").textContent = "Please log in again. " + error.message;
  }
}

async function loadAppData() {
  const data = await getJson("/api/app/bootstrap");
  state.user = data.user || null;
  state.portfolioClasses = data.portfolioClasses || [];
  state.expenses = data.expenses || [];
  state.watchlist = data.watchlist?.length ? data.watchlist : [];
  state.watchSymbol = state.watchlist[0] || "AAPL";
}

async function loadConfig() {
  try {
    const data = await getJson("/api/config");
    state.config = data;
    const label = `${data.realtimeProvider} market feed`;
    document.getElementById("providerLabel").textContent = label;
    document.getElementById("homeProviderLabel").textContent = label;
  } catch (_) {
    document.getElementById("providerLabel").textContent = "Market feed unavailable";
    document.getElementById("homeProviderLabel").textContent = "Market feed unavailable";
  }
}

function showPage(page) {
  document.querySelectorAll(".view").forEach((view) => view.classList.remove("active"));
  document.getElementById(`${page}View`).classList.add("active");
  const navPage = ["compare", "news"].includes(page) ? "overview" : page;
  document.querySelectorAll("[data-nav-page]").forEach((button) => {
    const active = button.dataset.navPage === navPage;
    button.classList.toggle("active", active);
    if (active) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  window.scrollTo({ top: 0, behavior: "smooth" });
  refreshActivePage(page).catch((error) => showToast(error.message, "error"));
}

function activePage() {
  return document.querySelector(".view.active")?.id?.replace("View", "") || "home";
}

async function refreshActivePage(page = activePage()) {
  if (page === "home") await renderHomeDashboard();
  if (page === "overview") {
    await renderMajorStocks();
    await loadOverviewStock(state.symbol);
  }
  if (page === "compare") await loadComparison();
  if (page === "news") await loadNews();
  if (page === "portfolio") await renderPortfolio();
  if (page === "buddy") renderBuddy();
  if (page === "watchlist") {
    await renderWatchlist();
    await loadWatchlistChart(state.watchSymbol || state.watchlist[0]);
  }
}

async function renderHomeDashboard() {
  const totals = portfolioTotals();
  const firstName = state.user?.fullName?.trim().split(/\s+/)[0] || "Investor";
  document.getElementById("welcomeTitle").textContent = `Welcome back, ${firstName}.`;
  document.getElementById("welcomeSubtitle").textContent =
    `${new Date().toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" })} · Your financial picture is ready.`;

  const grid = document.getElementById("homeSummaryGrid");
  grid.innerHTML = "";
  const savingsRate = totals.income > 0 ? Math.min(100, (totals.liquid / totals.income) * 10) : 0;
  [
    ["Net worth", totals.netWorth, "Assets after liabilities", "money"],
    ["Invested assets", totals.assets, `${totals.classes.filter((item) => !item.isLiability && item.total > 0).length} active asset classes`, "money"],
    ["Annual income", totals.income, "Across all income sources", "money"],
    ["Watchlist", state.watchlist.length, "Saved market symbols", "number"]
  ].forEach(([label, value, note, format]) => {
    const card = document.createElement("article");
    card.className = "metric-card senior-metric";
    card.innerHTML = `<span>${label}</span><strong>${format === "money" ? money.format(value) : number.format(value)}</strong><small>${note}</small>`;
    grid.appendChild(card);
  });

  const allocation = document.getElementById("homeAllocationPreview");
  const debtRatio = totals.assets > 0 ? Math.min(100, (totals.liabilities / totals.assets) * 100) : 0;
  const healthScore = Math.max(0, Math.round(100 - debtRatio));
  allocation.innerHTML = `
    <div class="health-stat"><strong>${healthScore}</strong><span>Financial health score</span></div>
    <div class="health-bar" aria-label="Financial health score ${healthScore} out of 100"><span style="width:${healthScore}%"></span></div>
    <p class="health-copy">${number.format(debtRatio)}% debt-to-asset ratio · ${number.format(savingsRate)}% liquidity coverage indicator.</p>
  `;

  await renderHomeWatchlist();
}

async function renderHomeWatchlist() {
  const preview = document.getElementById("homeWatchlistPreview");
  const symbols = state.watchlist.slice(0, 3);
  if (!symbols.length) {
    preview.innerHTML = '<div class="empty-state">Add a ticker to start your watchlist.</div>';
    return;
  }
  preview.innerHTML = symbols.map((symbol) => `
    <div class="quick-symbol skeleton"><div><strong>${escapeHtml(symbol)}</strong><small>Loading quote</small></div></div>
  `).join("");
  const results = await Promise.allSettled(symbols.map((symbol) => getJson(`/api/market/quote/${encodeURIComponent(symbol)}`)));
  preview.innerHTML = "";
  results.forEach((result, index) => {
    const symbol = symbols[index];
    const row = document.createElement("div");
    row.className = "quick-symbol";
    if (result.status === "fulfilled") {
      const quote = result.value;
      const change = finiteNumber(quote.changePercent);
      row.innerHTML = `
        <div><strong>${escapeHtml(symbol)}</strong><small>${escapeHtml(formatFreshness(quote.freshness))}</small></div>
        <div class="${change >= 0 ? "positive" : "negative"}"><strong>${money.format(finiteNumber(quote.price))}</strong><small>${change >= 0 ? "+" : ""}${change.toFixed(2)}%</small></div>
      `;
    } else {
      row.innerHTML = `<div><strong>${escapeHtml(symbol)}</strong><small>Quote temporarily unavailable</small></div>`;
    }
    preview.appendChild(row);
  });
}

function renderBuddy() {
  const today = todayIso();
  const todaySpend = state.expenses
    .filter((expense) => expense.date === today)
    .reduce((sum, expense) => sum + finiteNumber(expense.amount), 0);
  const monthSpend = state.expenses.reduce((sum, expense) => sum + finiteNumber(expense.amount), 0);
  const categoryTotals = totalsByCategory(state.expenses);
  const largestCategory = Object.entries(categoryTotals).sort((a, b) => b[1] - a[1])[0] || ["None", 0];
  const avgDaily = monthSpend / Math.max(new Set(state.expenses.map((expense) => expense.date)).size, 1);

  const grid = document.getElementById("buddySummaryGrid");
  grid.innerHTML = "";
  [
    ["Today", todaySpend, "Spending recorded today"],
    ["This month", monthSpend, "Mock July spending total"],
    ["Largest category", largestCategory[1], largestCategory[0]],
    ["Daily average", avgDaily, "Average across active days"]
  ].forEach(([label, value, note]) => {
    const card = document.createElement("article");
    card.className = "metric-card senior-metric";
    card.innerHTML = `<span>${label}</span><strong>${money.format(value)}</strong><small>${note}</small>`;
    grid.appendChild(card);
  });

  renderPieChart("expenseCategoryChart", Object.keys(categoryTotals), Object.values(categoryTotals));
  const dailyTotals = totalsByDate(state.expenses);
  renderBarChart("expenseTrendChart", Object.keys(dailyTotals), Object.values(dailyTotals), "Daily spend");
  renderExpenseHistory();
}

function renderExpenseHistory() {
  const history = document.getElementById("expenseHistory");
  history.innerHTML = `
    <div class="panel-header">
      <div>
        <p class="eyebrow">History</p>
        <h3>Recent expenses</h3>
      </div>
    </div>
  `;
  state.expenses.forEach((expense, index) => {
    const row = document.createElement("div");
    row.className = "expense-row";
    row.innerHTML = `
      <div>
        <strong>${escapeHtml(expense.name)}</strong>
        <small>${escapeHtml(expense.date)} · ${escapeHtml(expense.category)}</small>
      </div>
      <input type="number" min="0" step="1" value="${finiteNumber(expense.amount)}" data-expense-index="${index}">
    `;
    history.appendChild(row);
  });
  history.querySelectorAll("input").forEach((input) => {
    input.addEventListener("change", async (event) => {
      const expense = state.expenses[Number(event.target.dataset.expenseIndex)];
      expense.amount = finiteNumber(event.target.value);
      await apiJson(`/api/app/expenses/${expense.id}`, { method: "PUT", body: { amount: expense.amount } });
      renderBuddy();
    });
  });
}

async function renderMajorStocks() {
  const grid = document.getElementById("majorStockGrid");
  grid.innerHTML = state.majorStocks.map((symbol) => `
    <article class="stock-card skeleton"><span>Loading market quote</span><strong>${symbol}</strong><small>One moment</small></article>
  `).join("");
  const quotes = await quoteMany(state.majorStocks);
  grid.innerHTML = "";
  quotes.forEach((quote) => grid.appendChild(stockCard(quote, () => {
    state.symbol = quote.symbol;
    document.getElementById("symbolInput").value = quote.symbol;
    loadOverviewStock(quote.symbol);
  })));
  if (!quotes.length) grid.innerHTML = '<div class="empty-state">Market quotes are temporarily unavailable. Your portfolio data is unaffected.</div>';
}

async function loadOverviewStock(symbol) {
  state.symbol = cleanSymbol(symbol, "AAPL");
  document.getElementById("symbolInput").value = state.symbol;
  document.getElementById("chartTitle").textContent = `${state.symbol} trend`;
  const status = document.getElementById("overviewStatus");
  const submit = document.querySelector('#symbolForm button[type="submit"]');
  submit.disabled = true;
  status.textContent = `Loading ${state.symbol} from the market provider...`;
  try {
    const [quote, history] = await Promise.all([
      getJson(`/api/market/quote/${encodeURIComponent(state.symbol)}`),
      getJson(`/api/market/history/${encodeURIComponent(state.symbol)}?range=${state.range}`)
    ]);
    renderQuoteMetrics(quote);
    document.getElementById("chartProvider").textContent = marketSource(history, "price history");
    status.textContent = `${state.symbol} updated · ${marketSource(quote, "")}`;
    renderLineChart("priceChart", history.bars || [], [
      { label: "Close", key: "close", color: "#007aff", fill: true },
      { label: "High", key: "high", color: "#30d158" },
      { label: "Low", key: "low", color: "#ff3b30" }
    ], "Price");
  } catch (error) {
    status.textContent = `Could not load ${state.symbol}: ${error.message}`;
    showToast(`Market data unavailable for ${state.symbol}`, "error");
  } finally {
    submit.disabled = false;
  }
}

function renderQuoteMetrics(quote) {
  const price = finiteNumber(quote.price);
  const open = finiteNumber(quote.open);
  const high = finiteNumber(quote.high);
  const low = finiteNumber(quote.low);
  const volume = finiteNumber(quote.volume);
  const change = finiteNumber(quote.change);
  const changePercent = finiteNumber(quote.changePercent);
  document.getElementById("lastPrice").textContent = money.format(price);
  document.getElementById("openPrice").textContent = money.format(open);
  document.getElementById("highLow").textContent = `${money.format(high)} / ${money.format(low)}`;
  document.getElementById("volume").textContent = number.format(volume);
  const changeNode = document.getElementById("priceChange");
  changeNode.textContent = `${change >= 0 ? "+" : ""}${money.format(change)} (${changePercent.toFixed(2)}%)`;
  changeNode.className = change >= 0 ? "positive" : "negative";
}

async function loadNews() {
  const list = document.getElementById("newsList");
  list.innerHTML = Array.from({ length: 6 }, () => '<article class="news-card skeleton"><small>Loading</small><h4>Market headline</h4><p>Fetching the latest provider news.</p></article>').join("");
  let news;
  try {
    news = await getJson("/api/market/news");
  } catch (error) {
    list.innerHTML = `<div class="empty-state">${escapeHtml(error.message)}</div>`;
    document.getElementById("newsProvider").textContent = "News unavailable";
    return;
  }
  document.getElementById("newsProvider").textContent = `${news.provider} explore`;
  document.getElementById("newsTitle").textContent = "General market headlines";
  list.innerHTML = "";
  (news.articles || []).forEach((article) => {
    const card = document.createElement("article");
    card.className = "news-card";
    card.innerHTML = `
      <small>${escapeHtml(article.source || "Market news")} · ${escapeHtml(formatPublished(article.publishedAt))}</small>
      <h4>${escapeHtml(article.title || "Untitled")}</h4>
      <p>${escapeHtml(article.summary || "")}</p>
      <a href="${escapeAttribute(article.url || "#")}" target="_blank" rel="noreferrer">Read article</a>
    `;
    list.appendChild(card);
  });
  if (!news.articles?.length) list.innerHTML = '<div class="empty-state">No current market headlines were returned.</div>';
}

async function loadComparison() {
  const symbols = document.getElementById("compareInput").value.replace(/\s+/g, "").toUpperCase() || "AAPL,MSFT,NVDA";
  const providerLabel = document.getElementById("compareProvider");
  providerLabel.textContent = "Loading live market data...";
  let data;
  try {
    data = await getJson(`/api/market/compare?symbols=${encodeURIComponent(symbols)}&range=${state.range}`);
  } catch (error) {
    providerLabel.textContent = `Market data unavailable · ${error.message}`;
    renderLineChart("compareChart", [], [], "Return %");
    return;
  }
  const sources = [...new Set((data.series || []).map((item) => `${item.provider} ${formatFreshness(item.freshness)}`))];
  providerLabel.textContent = sources.join(" · ") || "Market comparison";
  const colors = ["#007aff", "#30d158", "#ff9500", "#ff3b30", "#64d2ff"];
  const datasets = (data.series || []).map((item, index) => {
    const first = item.bars?.[0]?.close || 1;
    return {
      label: item.symbol,
      data: (item.bars || []).map((bar) => ({ ...bar, normalized: ((bar.close - first) / first) * 100 })),
      key: "normalized",
      color: colors[index % colors.length]
    };
  });
  renderLineChart("compareChart", datasets[0]?.data || [], datasets, "Return %");
}

async function renderPortfolio() {
  const totals = portfolioTotals();
  renderPortfolioSummary(totals);
  renderPortfolioCharts(totals);
  renderAssetClassDetails(totals);
}

function portfolioTotals() {
  const classes = state.portfolioClasses.map((assetClass) => {
    const total = assetClass.items.reduce((sum, item) => sum + finiteNumber(item.value), 0);
    const income = assetClass.items.reduce((sum, item) => sum + finiteNumber(item.income), 0);
    return { ...assetClass, total, income };
  });
  const assets = classes.filter((item) => !item.isLiability).reduce((sum, item) => sum + item.total, 0);
  const liabilities = classes.filter((item) => item.isLiability).reduce((sum, item) => sum + item.total, 0);
  const income = classes.reduce((sum, item) => sum + item.income, 0);
  const liquid = classes
    .filter((item) => item.isLiquid)
    .reduce((sum, item) => sum + item.total, 0);
  const housing = classes.find((item) => item.id === "housing")?.total || 0;
  return { classes, assets, liabilities, netWorth: assets - liabilities, income, liquid, housing };
}

function renderPortfolioSummary(totals) {
  const grid = document.getElementById("portfolioSummaryGrid");
  grid.innerHTML = "";
  [
    ["Net worth", totals.netWorth, "Assets minus loans and debts"],
    ["Liquid assets", totals.liquid, "Cash, securities, bonds, funds"],
    ["Yearly income", totals.income, "Pensions, annuities, rent, yield"],
    ["Housing value", totals.housing, "Home and real estate exposure"],
    ["Loans and debts", totals.liabilities, "Balances reducing net worth"]
  ].forEach(([label, value, note]) => {
    const card = document.createElement("article");
    card.className = "metric-card senior-metric";
    card.innerHTML = `<span>${label}</span><strong>${money.format(value)}</strong><small>${note}</small>`;
    grid.appendChild(card);
  });
}

function renderPortfolioCharts(totals) {
  const assetClasses = totals.classes.filter((item) => !item.isLiability && item.total > 0);
  renderPieChart("allocationChart", assetClasses.map((item) => item.name), assetClasses.map((item) => item.total));
  renderBarChart("classContributionChart", assetClasses.map((item) => item.name), assetClasses.map((item) => item.total), "Value");
  renderPieChart("assetDebtChart", ["Assets", "Loans and debts"], [totals.assets, totals.liabilities]);
}

function renderAssetClassDetails(totals) {
  const list = document.getElementById("assetClassList");
  list.innerHTML = "";
  totals.classes.forEach((assetClass) => {
    const section = document.createElement("section");
    section.className = `asset-class-card ${assetClass.isLiability ? "liability-card" : ""}`;
    const rows = assetClass.items.map((item, index) => `
      <tr>
        <td>
          <strong>${escapeHtml(item.name)}</strong>
          <small>${escapeHtml(item.note)}</small>
        </td>
        <td>
          <input class="inline-money-input" type="number" min="0" step="500" value="${finiteNumber(item.value)}" data-class-id="${assetClass.id}" data-item-index="${index}" data-field="value">
        </td>
        <td>
          <input class="inline-money-input" type="number" min="0" step="100" value="${finiteNumber(item.income)}" data-class-id="${assetClass.id}" data-item-index="${index}" data-field="income">
        </td>
        <td>${assetClass.total ? ((finiteNumber(item.value) / assetClass.total) * 100).toFixed(1) : "0.0"}%</td>
      </tr>
    `).join("");

    section.innerHTML = `
      <div class="asset-class-header">
        <div>
          <p class="eyebrow">${assetClass.isLiability ? "Liability" : "Asset class"}</p>
          <h3>${escapeHtml(assetClass.name)}</h3>
          <p>${escapeHtml(assetClass.purpose)}</p>
        </div>
        <div class="class-total">
          <strong>${money.format(assetClass.total)}</strong>
          <small>${money.format(assetClass.income)} yearly income</small>
        </div>
      </div>
      <div class="table-panel compact-table">
        <table>
          <thead>
            <tr><th>Item</th><th>Value / balance</th><th>Yearly income</th><th>Class share</th></tr>
          </thead>
          <tbody>${rows}</tbody>
        </table>
      </div>
    `;
    list.appendChild(section);
  });

  document.querySelectorAll(".inline-money-input").forEach((input) => {
    input.addEventListener("change", async (event) => {
      const classId = event.target.dataset.classId;
      const itemIndex = Number(event.target.dataset.itemIndex);
      const field = event.target.dataset.field;
      const assetClass = state.portfolioClasses.find((item) => item.id === classId);
      assetClass.items[itemIndex][field] = finiteNumber(event.target.value);
      const item = assetClass.items[itemIndex];
      await apiJson(`/api/app/assets/${item.id}`, {
        method: "PUT",
        body: { value: finiteNumber(item.value), income: finiteNumber(item.income) }
      });
      renderPortfolio();
    });
  });
}

async function renderWatchlist() {
  const grid = document.getElementById("watchlistGrid");
  grid.innerHTML = "";
  if (!state.watchlist.length) {
    grid.innerHTML = '<div class="empty-state"><strong>Your watchlist is empty.</strong><br>Search for a ticker above to add your first security.</div>';
    return;
  }
  const requests = state.watchlist.map((symbol) => {
    const card = savedStockCard(symbol);
    grid.appendChild(card);
    return getJson(`/api/market/quote/${encodeURIComponent(symbol)}`)
      .then((quote) => card.replaceWith(watchlistStockCard(quote)))
      .catch(() => {
        card.querySelector("span").textContent = "Saved · quote unavailable";
        card.querySelector("small").textContent = "Click to retry the chart";
      });
  });
  await Promise.allSettled(requests);
}

function savedStockCard(symbol) {
  const card = document.createElement("article");
  card.className = "watchlist-card skeleton";
  card.innerHTML = `
    <span>Saved · loading quote</span>
    <strong>${escapeHtml(symbol)}</strong>
    <small>Loading market data...</small>
  `;
  return card;
}

function watchlistStockCard(quote) {
  const card = document.createElement("article");
  const change = finiteNumber(quote.changePercent);
  card.className = "watchlist-card";
  card.innerHTML = `
    <button class="watchlist-card-main" type="button">
      <span>${escapeHtml(marketSource(quote, "quote"))}</span>
      <strong>${escapeHtml(quote.symbol)} · ${money.format(finiteNumber(quote.price))}</strong>
      <small class="${change >= 0 ? "positive" : "negative"}">${change >= 0 ? "+" : ""}${change.toFixed(2)}%</small>
    </button>
    <div class="watchlist-card-actions">
      <small>Open chart</small>
      <button class="remove-action" type="button" aria-label="Remove ${escapeAttribute(quote.symbol)} from watchlist">Remove</button>
    </div>
  `;
  card.querySelector(".watchlist-card-main").addEventListener("click", () => loadWatchlistChart(quote.symbol));
  card.querySelector(".remove-action").addEventListener("click", () => removeWatchlistSymbol(quote.symbol));
  return card;
}

async function removeWatchlistSymbol(symbol) {
  const status = document.getElementById("watchlistStatus");
  status.textContent = `Removing ${symbol}...`;
  try {
    await apiJson(`/api/app/watchlist/${encodeURIComponent(symbol)}`, { method: "DELETE" });
    state.watchlist = state.watchlist.filter((item) => item !== symbol);
    state.watchSymbol = state.watchlist[0] || "AAPL";
    await renderWatchlist();
    status.textContent = `${symbol} removed from your watchlist.`;
    showToast(`${symbol} removed`, "success");
    if (state.watchlist.length) await loadWatchlistChart(state.watchSymbol);
  } catch (error) {
    status.textContent = `Could not remove ${symbol}: ${error.message}`;
    showToast(`Could not remove ${symbol}`, "error");
  }
}

async function loadWatchlistChart(symbol) {
  state.watchSymbol = cleanSymbol(symbol, state.watchlist[0] || "AAPL");
  document.getElementById("watchChartProvider").textContent = "Loading market history...";
  try {
    const history = await getJson(`/api/market/history/${encodeURIComponent(state.watchSymbol)}?range=${state.range}`);
    document.getElementById("watchChartProvider").textContent = marketSource(history, "price history");
    document.getElementById("watchChartTitle").textContent = `${state.watchSymbol} trend`;
    renderLineChart("watchChart", history.bars || [], [
      { label: "Close", key: "close", color: "#007aff", fill: true },
      { label: "High", key: "high", color: "#30d158" },
      { label: "Low", key: "low", color: "#ff3b30" }
    ], "Price");
  } catch (error) {
    document.getElementById("watchChartProvider").textContent = `Chart unavailable · ${error.message}`;
    throw error;
  }
}

async function quoteMany(symbols) {
  const settled = await Promise.allSettled(symbols.map((symbol) => getJson(`/api/market/quote/${symbol}`)));
  return settled
    .filter((result) => result.status === "fulfilled")
    .map((result) => result.value);
}

function stockCard(quote, onSelect) {
  const card = document.createElement("button");
  const change = finiteNumber(quote.changePercent);
  card.className = "stock-card";
  card.type = "button";
  card.innerHTML = `
    <span>${escapeHtml(marketSource(quote, "quote"))}</span>
    <strong>${escapeHtml(quote.symbol)} · ${money.format(finiteNumber(quote.price))}</strong>
    <small class="${change >= 0 ? "positive" : "negative"}">${change >= 0 ? "+" : ""}${change.toFixed(2)}%</small>
  `;
  card.addEventListener("click", onSelect);
  return card;
}

function renderLineChart(canvasId, bars, datasetSpecs, yTitle) {
  const canvas = document.getElementById(canvasId);
  if (!bars.length) {
    if (state.charts[canvasId]) state.charts[canvasId].destroy();
    delete state.charts[canvasId];
    drawEmptyChart(canvas, "No chart data available");
    return;
  }
  const labels = bars.map(labelForBar);
  const datasets = datasetSpecs.map((spec) => ({
    label: spec.label,
    data: (spec.data || bars).map((bar) => finiteNumber(bar[spec.key])),
    borderColor: spec.color,
    backgroundColor: spec.fill ? "rgba(0,122,255,0.12)" : "transparent",
    tension: 0.28,
    pointRadius: 0,
    fill: Boolean(spec.fill)
  }));
  if (typeof Chart === "undefined") {
    drawFallbackChart(canvas, labels, datasets, yTitle);
    return;
  }
  if (state.charts[canvasId]) state.charts[canvasId].destroy();
  state.charts[canvasId] = new Chart(canvas, {
    type: "line",
    data: { labels, datasets },
    options: chartOptions(yTitle)
  });
}

function drawEmptyChart(canvas, message) {
  const width = canvas.parentElement.clientWidth || 700;
  const ratio = window.devicePixelRatio || 1;
  canvas.width = width * ratio;
  canvas.height = 300 * ratio;
  canvas.style.width = `${width}px`;
  canvas.style.height = "300px";
  const ctx = canvas.getContext("2d");
  ctx.scale(ratio, ratio);
  ctx.clearRect(0, 0, width, 300);
  ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--color-text-secondary").trim();
  ctx.font = "600 14px -apple-system, BlinkMacSystemFont, sans-serif";
  ctx.textAlign = "center";
  ctx.fillText(message, width / 2, 150);
}

function renderPieChart(canvasId, labels, values) {
  const canvas = document.getElementById(canvasId);
  const colors = ["#007aff", "#30d158", "#ff9500", "#ff3b30", "#64d2ff", "#5856d6", "#8e8e93", "#af52de", "#34c759"];
  if (typeof Chart === "undefined") {
    drawFallbackPie(canvas, labels, values, colors);
    return;
  }
  if (state.charts[canvasId]) state.charts[canvasId].destroy();
  state.charts[canvasId] = new Chart(canvas, {
    type: "doughnut",
    data: {
      labels,
      datasets: [{ data: values, backgroundColor: labels.map((_, index) => colors[index % colors.length]), borderWidth: 0 }]
    },
    options: {
      responsive: true,
      maintainAspectRatio: false,
      plugins: { legend: { position: "bottom", labels: { usePointStyle: true, boxWidth: 8 } } },
      cutout: "62%"
    }
  });
}

function renderBarChart(canvasId, labels, values, yTitle) {
  const canvas = document.getElementById(canvasId);
  if (typeof Chart === "undefined") {
    drawFallbackBars(canvas, labels, values, yTitle);
    return;
  }
  if (state.charts[canvasId]) state.charts[canvasId].destroy();
  state.charts[canvasId] = new Chart(canvas, {
    type: "bar",
    data: {
      labels,
      datasets: [{ label: yTitle, data: values, backgroundColor: "rgba(0,122,255,0.72)", borderRadius: 6 }]
    },
    options: chartOptions(yTitle)
  });
}

function chartOptions(yTitle) {
  return {
    responsive: true,
    maintainAspectRatio: false,
    interaction: { mode: "index", intersect: false },
    plugins: {
      legend: { labels: { usePointStyle: true, boxWidth: 8 } },
      tooltip: { padding: 12, displayColors: true }
    },
    scales: {
      x: { grid: { display: false }, ticks: { maxTicksLimit: 8 } },
      y: { title: { display: true, text: yTitle }, grid: { color: "rgba(142,142,147,0.16)" } }
    }
  };
}

function drawFallbackChart(canvas, labels, datasets, yTitle) {
  const parentWidth = canvas.parentElement.clientWidth || 900;
  canvas.width = parentWidth * window.devicePixelRatio;
  canvas.height = 360 * window.devicePixelRatio;
  canvas.style.width = `${parentWidth}px`;
  canvas.style.height = "360px";
  const ctx = canvas.getContext("2d");
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  const width = parentWidth;
  const height = 360;
  const padding = { top: 24, right: 24, bottom: 42, left: 58 };
  const allValues = datasets.flatMap((set) => set.data).filter((value) => Number.isFinite(value));
  const min = Math.min(...allValues);
  const max = Math.max(...allValues);
  const span = max - min || 1;

  ctx.clearRect(0, 0, width, height);
  ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--color-text-secondary").trim();
  ctx.font = "12px -apple-system, BlinkMacSystemFont, sans-serif";
  ctx.fillText(yTitle, 14, 22);
  ctx.strokeStyle = "rgba(142,142,147,0.16)";
  ctx.lineWidth = 1;

  for (let i = 0; i <= 4; i++) {
    const y = padding.top + ((height - padding.top - padding.bottom) / 4) * i;
    ctx.beginPath();
    ctx.moveTo(padding.left, y);
    ctx.lineTo(width - padding.right, y);
    ctx.stroke();
  }

  datasets.forEach((set) => {
    ctx.strokeStyle = set.borderColor || set.color;
    ctx.lineWidth = 2.5;
    ctx.beginPath();
    set.data.forEach((value, index) => {
      const x = padding.left + ((width - padding.left - padding.right) * index) / Math.max(set.data.length - 1, 1);
      const y = height - padding.bottom - ((value - min) / span) * (height - padding.top - padding.bottom);
      if (index === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();
  });

  ctx.fillText(labels[0] || "", padding.left, height - 14);
  ctx.fillText(labels[labels.length - 1] || "", width - padding.right - 44, height - 14);
}

function drawFallbackPie(canvas, labels, values, colors) {
  const parentWidth = canvas.parentElement.clientWidth || 520;
  canvas.width = parentWidth * window.devicePixelRatio;
  canvas.height = 360 * window.devicePixelRatio;
  canvas.style.width = `${parentWidth}px`;
  canvas.style.height = "360px";
  const ctx = canvas.getContext("2d");
  ctx.scale(window.devicePixelRatio, window.devicePixelRatio);
  const total = values.reduce((sum, value) => sum + finiteNumber(value), 0) || 1;
  const centerX = parentWidth / 2;
  const centerY = 150;
  const radius = 96;
  let start = -Math.PI / 2;
  values.forEach((value, index) => {
    const angle = (finiteNumber(value) / total) * Math.PI * 2;
    ctx.beginPath();
    ctx.moveTo(centerX, centerY);
    ctx.arc(centerX, centerY, radius, start, start + angle);
    ctx.closePath();
    ctx.fillStyle = colors[index % colors.length];
    ctx.fill();
    start += angle;
  });
  ctx.beginPath();
  ctx.arc(centerX, centerY, 58, 0, Math.PI * 2);
  ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--color-background").trim();
  ctx.fill();
  ctx.font = "12px -apple-system, BlinkMacSystemFont, sans-serif";
  labels.slice(0, 5).forEach((label, index) => {
    ctx.fillStyle = colors[index % colors.length];
    ctx.fillRect(18, 268 + index * 17, 10, 10);
    ctx.fillStyle = getComputedStyle(document.documentElement).getPropertyValue("--color-text-secondary").trim();
    ctx.fillText(label, 36, 277 + index * 17);
  });
}

function drawFallbackBars(canvas, labels, values, yTitle) {
  const bars = labels.map((label, index) => ({ label, value: finiteNumber(values[index]) }));
  drawFallbackChart(canvas, labels, [{ label: yTitle, data: bars.map((bar) => bar.value), borderColor: "#007aff" }], yTitle);
}

function bindTickerSearch(inputId, menuId) {
  const input = document.getElementById(inputId);
  const menu = document.getElementById(menuId);
  input.addEventListener("input", () => {
    const query = input.value.trim();
    clearTimeout(searchTimers.get(inputId));
    if (query.length < 1) {
      hideSuggestions(inputId, menuId);
      return;
    }
    const timer = setTimeout(async () => {
      menu.classList.remove("hidden");
      menu.innerHTML = '<div class="suggestion-item skeleton">Searching market symbols...</div>';
      input.setAttribute("aria-expanded", "true");
      try {
        const data = await getJson(`/api/market/search?q=${encodeURIComponent(query)}`);
        renderTickerSuggestions(inputId, menuId, data.results || []);
      } catch (error) {
        menu.innerHTML = `<div class="suggestion-item">${escapeHtml(error.message)}</div>`;
      }
    }, 320);
    searchTimers.set(inputId, timer);
  });

  input.addEventListener("keydown", (event) => {
    if (event.key === "Escape") hideSuggestions(inputId, menuId);
  });
}

function renderTickerSuggestions(inputId, menuId, results) {
  const input = document.getElementById(inputId);
  const menu = document.getElementById(menuId);
  menu.innerHTML = "";
  if (!results.length) {
    menu.innerHTML = '<div class="suggestion-item">No matching ticker found.</div>';
    return;
  }
  results.slice(0, 7).forEach((result) => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "suggestion-item";
    button.setAttribute("role", "option");
    button.innerHTML = `
      <span><strong>${escapeHtml(result.symbol)}</strong><small>${escapeHtml(result.name || "Listed security")}</small></span>
      <span class="suggestion-region">${escapeHtml(result.region || "")}</span>
    `;
    button.addEventListener("click", () => {
      input.value = result.symbol;
      hideSuggestions(inputId, menuId);
      input.focus();
    });
    menu.appendChild(button);
  });
}

function hideSuggestions(inputId, menuId) {
  const input = document.getElementById(inputId);
  const menu = document.getElementById(menuId);
  menu.classList.add("hidden");
  menu.innerHTML = "";
  input.setAttribute("aria-expanded", "false");
}

function initializeTheme() {
  const saved = localStorage.getItem("tangent-theme");
  if (saved === "light" || saved === "dark") document.documentElement.dataset.theme = saved;
}

function toggleTheme() {
  const current = document.documentElement.dataset.theme
    || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  const next = current === "dark" ? "light" : "dark";
  document.documentElement.dataset.theme = next;
  localStorage.setItem("tangent-theme", next);
  showToast(`${next === "dark" ? "Dark" : "Light"} theme enabled`, "success");
}

function showToast(message, type = "info") {
  const region = document.getElementById("toastRegion");
  const toast = document.createElement("div");
  toast.className = `toast ${type}`;
  toast.textContent = message;
  region.appendChild(toast);
  setTimeout(() => toast.remove(), 3600);
}

async function getJson(url) {
  return apiJson(url);
}

async function apiJson(url, options = {}) {
  const headers = { ...(options.body ? { "Content-Type": "application/json" } : {}) };
  if (options.authenticated !== false) {
    const token = localStorage.getItem("tangent-session");
    if (token) headers.Authorization = `Bearer ${token}`;
  }
  const response = await fetch(url, {
    method: options.method || "GET",
    headers,
    body: options.body ? JSON.stringify(options.body) : undefined
  });
  const responseText = await response.text();
  if (!response.ok) {
    let message = `Request failed: ${response.status}`;
    if (responseText) {
      try {
        const error = JSON.parse(responseText);
        message = error.message || error.error || message;
      } catch (_) {
        message = responseText;
      }
    }
    throw new Error(message);
  }
  if (!responseText.trim()) return null;
  const payload = JSON.parse(responseText);
  return payload && payload.success === true && Object.prototype.hasOwnProperty.call(payload, "data")
    ? payload.data
    : payload;
}

function cleanSymbol(value, fallback) {
  return String(value || fallback).trim().toUpperCase().replace(/[^A-Z0-9.-]/g, "") || fallback;
}

function formatFreshness(value) {
  return String(value || "").toLowerCase().replaceAll("_", " ");
}

function marketSource(data, suffix) {
  const provider = data?.provider || "Market";
  const freshness = formatFreshness(data?.freshness);
  return `${provider}${freshness ? ` · ${freshness}` : ""} ${suffix}`;
}

function finiteNumber(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function totalsByCategory(expenses) {
  return expenses.reduce((totals, expense) => {
    totals[expense.category] = (totals[expense.category] || 0) + finiteNumber(expense.amount);
    return totals;
  }, {});
}

function totalsByDate(expenses) {
  return expenses.reduce((totals, expense) => {
    totals[expense.date] = (totals[expense.date] || 0) + finiteNumber(expense.amount);
    return totals;
  }, {});
}

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function labelForBar(bar) {
  if (bar.date) return bar.date.slice(5);
  return new Date(bar.time).toLocaleDateString("en-US", { month: "short", day: "numeric" });
}

function formatPublished(value) {
  if (!value) return "Recent";
  if (/^\d{8}T/.test(value)) return `${value.slice(0, 4)}-${value.slice(4, 6)}-${value.slice(6, 8)}`;
  return value.slice(0, 10);
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#039;"
  }[char]));
}

function escapeAttribute(value) {
  return escapeHtml(value).replace(/`/g, "&#096;");
}
