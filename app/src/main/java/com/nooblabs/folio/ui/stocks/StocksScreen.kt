package com.nooblabs.folio.ui.stocks

import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooblabs.folio.domain.model.StockInvestment
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StocksScreen(viewModel: StocksViewModel, onNavigateToSettings: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    var showStockSheet by remember { mutableStateOf(false) }
    var editingStock by remember { mutableStateOf<StockInvestment?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingStock = null
                    showStockSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Stock")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.groupedStocks.isEmpty() && uiState.isApiKeySet) {
                EmptyStocksState()
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!uiState.isApiKeySet) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Finnhub API Key Missing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "To fetch real-time stock prices, please add your own Finnhub API Key in Settings.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    if (uiState.groupedStocks.isEmpty()) {
                        Box(modifier = Modifier.weight(1f)) {
                            EmptyStocksState()
                        }
                    } else {
                        val formattedTime = uiState.lastUpdated?.let {
                            java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(java.util.Date(it))
                        } ?: "Never"
                        
                        Text(
                        text = "Last Updated at: $formattedTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.groupedStocks.forEach { (ticker, entries) ->
                            val firstEntry = entries.first()
                            val totalValue = entries.sumOf { it.convertedTotalValue }
                            val totalCost = entries.sumOf { it.convertedTotalCost }
                            
                            item(key = "header_$ticker") {
                                StockGroupHeader(
                                    ticker = ticker,
                                    currentPrice = firstEntry.convertedCurrentPrice,
                                    totalValue = totalValue,
                                    totalCost = totalCost,
                                    globalCurrency = uiState.globalCurrency,
                                    isApiKeySet = uiState.isApiKeySet
                                )
                            }
                            
                            items(entries, key = { it.stock.id }) { stockUiModel ->
                                var isVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    isVisible = true
                                }
                                AnimatedVisibility(
                                    visible = isVisible,
                                    enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                                        animationSpec = tween(500),
                                        initialOffsetY = { it / 2 }
                                    )
                                ) {
                                    StockLotCard(
                                        stockUiModel = stockUiModel,
                                        isApiKeySet = uiState.isApiKeySet,
                                        onClick = {
                                            editingStock = stockUiModel.stock
                                            showStockSheet = true
                                        }
                                    )
                                }
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                    }
                }
            }
        }
    }
}
}

    if (showStockSheet) {
        StockFormBottomSheet(
            stockToEdit = editingStock,
            onDismiss = { showStockSheet = false },
            onSave = { ticker, quantity, avgPrice, purchaseDate ->
                if (editingStock != null) {
                    viewModel.updateStock(editingStock!!.id, ticker, quantity, avgPrice, purchaseDate)
                } else {
                    viewModel.addStock(ticker, quantity, avgPrice, purchaseDate)
                }
                showStockSheet = false
            },
            cachedPrices = uiState.prices,
            globalCurrency = uiState.globalCurrency,
            onFetchPrice = { ticker -> viewModel.fetchSinglePrice(ticker) },
            isApiKeySet = uiState.isApiKeySet
        )
    }
}

@Composable
fun EmptyStocksState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ShowChart,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Stock Investments",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add your first stock and start tracking your portfolio.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun StockGroupHeader(
    ticker: String,
    currentPrice: Double,
    totalValue: Double,
    totalCost: Double,
    globalCurrency: String,
    isApiKeySet: Boolean = true
) {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = java.util.Currency.getInstance(globalCurrency)
    }
    val noPriceData = !isApiKeySet || currentPrice == 0.0
    val difference = totalValue - totalCost
    val isProfit = totalValue >= totalCost
    val profitColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)
    val differencePercent = if (totalCost > 0) (difference / totalCost) * 100 else 0.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = ticker,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (noPriceData) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = if (!isApiKeySet) "API key not configured" else "Price unavailable",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Text(
                    text = "Live Price: ${format.format(currentPrice)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            if (noPriceData) {
                Text(
                    text = "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = format.format(totalValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isProfit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                        contentDescription = null,
                        tint = profitColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${if (isProfit) "+" else ""}${format.format(difference)} (${String.format("%.2f%%", differencePercent)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = profitColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun StockLotCard(
    stockUiModel: StockUiModel,
    isApiKeySet: Boolean = true,
    onClick: () -> Unit
) {
    val stock = stockUiModel.stock
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = java.util.Currency.getInstance(stockUiModel.globalCurrency)
    }
    val noPriceData = !isApiKeySet || stockUiModel.convertedCurrentPrice == 0.0
    val difference = stockUiModel.convertedTotalValue - stockUiModel.convertedTotalCost
    val isProfit = stockUiModel.convertedTotalValue >= stockUiModel.convertedTotalCost
    val profitColor = if (isProfit) Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (noPriceData)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stock.quantity} Shares",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Bought @ ${format.format(stockUiModel.convertedAverageBuyPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val lotTime = java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(java.util.Date(stock.purchaseDate))
                    Text(
                        text = "Bought on: $lotTime",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (noPriceData) {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = format.format(stockUiModel.convertedTotalValue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${if (isProfit) "+" else ""}${format.format(difference)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = profitColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            if (noPriceData) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (!isApiKeySet)
                                "Add a Finnhub API key in Settings to see live value"
                            else
                                "Price data unavailable",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockFormBottomSheet(
    stockToEdit: StockInvestment?,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Long) -> Unit,
    cachedPrices: Map<String, Double>,
    globalCurrency: String,
    onFetchPrice: suspend (String) -> Double?,
    isApiKeySet: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var ticker by remember { mutableStateOf(stockToEdit?.tickerSymbol ?: "") }
    var quantityText by remember { 
        mutableStateOf(stockToEdit?.quantity?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "") 
    }
    var avgPriceText by remember { 
        mutableStateOf(stockToEdit?.averageBuyPrice?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "") 
    }
    var purchaseDate by remember { mutableStateOf(stockToEdit?.purchaseDate ?: System.currentTimeMillis()) }

    val isEditMode = stockToEdit != null
    var selectedCurrency by remember { mutableStateOf(globalCurrency) }
    var isFetchingPrice by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val format = remember(globalCurrency) {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
            currency = java.util.Currency.getInstance(globalCurrency)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp, top = 8.dp)
        ) {
            Text(
                text = if (isEditMode) "Edit Stock" else "Add Stock",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Ticker Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                OutlinedTextField(
                    value = ticker,
                    onValueChange = { ticker = it.uppercase() },
                    label = { Text("Ticker Symbol") },
                    placeholder = { Text("e.g. AAPL") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                if (isApiKeySet && ticker.isNotBlank()) {
                    if (isFetchingPrice) {
                        Box(
                            modifier = Modifier.height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                isFetchingPrice = true
                                coroutineScope.launch {
                                    val price = onFetchPrice(ticker)
                                    if (price != null && price > 0.0) {
                                        val converted = com.nooblabs.folio.domain.util.CurrencyConverter.convert(price, "USD", selectedCurrency)
                                        avgPriceText = String.format(Locale.US, "%.2f", converted)
                                    }
                                    isFetchingPrice = false
                                }
                            },
                            modifier = Modifier.height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Fetch")
                        }
                    }
                }
            }

            // Live Price display block
            val cleanTicker = ticker.trim().uppercase()
            val cachedPrice = cachedPrices[cleanTicker]
            if (cachedPrice != null && cachedPrice > 0.0) {
                val converted = com.nooblabs.folio.domain.util.CurrencyConverter.convert(cachedPrice, "USD", globalCurrency)
                Text(
                    text = "Live Price: ${format.format(converted)} ($globalCurrency)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            } else if (!isApiKeySet && ticker.isNotBlank()) {
                Text(
                    text = "Setup API Key in Settings to fetch live price.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Quantity") },
                placeholder = { Text("e.g. 10.5") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                var expanded by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.weight(0.4f)) {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(selectedCurrency, style = MaterialTheme.typography.bodyLarge)
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        com.nooblabs.folio.domain.util.CurrencyConverter.supportedCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency) },
                                onClick = {
                                    selectedCurrency = currency
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = avgPriceText,
                    onValueChange = { avgPriceText = it },
                    label = { Text("Avg Buy Price") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val dateForm = java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

            OutlinedTextField(
                value = dateForm.format(java.util.Date(purchaseDate)),
                onValueChange = {},
                label = { Text("Purchase Date & Time") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = purchaseDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        val newCal = java.util.Calendar.getInstance().apply {
                                            set(year, month, day, hour, minute)
                                        }
                                        purchaseDate = newCal.timeInMillis
                                    },
                                    calendar.get(java.util.Calendar.HOUR_OF_DAY),
                                    calendar.get(java.util.Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            calendar.get(java.util.Calendar.YEAR),
                            calendar.get(java.util.Calendar.MONTH),
                            calendar.get(java.util.Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                readOnly = true,
                enabled = false,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val quantity = quantityText.toDoubleOrNull() ?: 0.0
                    val avgPriceInCurrency = avgPriceText.toDoubleOrNull() ?: 0.0
                    val avgPriceInUsd = com.nooblabs.folio.domain.util.CurrencyConverter
                        .convert(avgPriceInCurrency, selectedCurrency, "USD")
                    if (ticker.isNotBlank() && quantity > 0) {
                        onSave(ticker, quantity, avgPriceInUsd, purchaseDate)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                enabled = ticker.isNotBlank() && quantityText.isNotBlank() && avgPriceText.isNotBlank()
            ) {
                Text(
                    text = if (isEditMode) "Update Stock" else "Save Stock",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
