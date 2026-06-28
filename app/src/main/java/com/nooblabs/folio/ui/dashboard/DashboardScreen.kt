package com.nooblabs.folio.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooblabs.folio.domain.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel, onNavigateToScreen: (String) -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        startAnimation = true
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alphaAnim"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.95f,
        animationSpec = tween(durationMillis = 800),
        label = "scaleAnim"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        if (uiState.userName.isNotBlank()) {
                            Text(
                                text = "Hi, ${uiState.userName}! 👋",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text("Folio Summary", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    var expanded by remember { mutableStateOf(false) }
                    val currencies = com.nooblabs.folio.domain.util.CurrencyConverter.supportedCurrencies
                    Box {
                        TextButton(onClick = { expanded = true }) {
                            Text("${uiState.globalCurrency} ▼", color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        viewModel.setGlobalCurrency(currency)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { onNavigateToScreen("settings") }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .alpha(alphaAnim)
                    .scale(scaleAnim),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    NetWorthCard(netWorth = uiState.netWorth, currencyCode = uiState.globalCurrency)
                }
                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "Banks",
                            amount = uiState.totalBankBalance,
                            currencyCode = uiState.globalCurrency,
                            icon = Icons.Filled.AccountBalance,
                            color = Color(0xFF4CAF50),
                            onClick = { onNavigateToScreen("assets?tab=0") }
                        )
                        SummaryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "Stocks",
                            amount = uiState.totalStockValue,
                            currencyCode = uiState.globalCurrency,
                            icon = Icons.Filled.ShowChart,
                            color = Color(0xFF2196F3),
                            onClick = { onNavigateToScreen("assets?tab=1") }
                        )
                        SummaryMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "Credit",
                            amount = uiState.totalCreditOutstanding,
                            currencyCode = uiState.globalCurrency,
                            icon = Icons.Filled.CreditCard,
                            color = Color(0xFFF44336),
                            onClick = { onNavigateToScreen("assets?tab=2") }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recent transactions",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(uiState.recentTransactions) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            currencyCode = uiState.globalCurrency,
                            onClick = { onNavigateToScreen("transactions") }
                        )
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun NetWorthCard(netWorth: Double, currencyCode: String) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Column {
                Text(
                    text = "Total Net Worth",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = formatCurrency(netWorth, currencyCode),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    amount: Double,
    currencyCode: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrencyCompact(amount, currencyCode),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction, currencyCode: String, onClick: () -> Unit = {}) {
    val isCredit = transaction.type.uppercase() == "CREDIT"
    val icon = if (isCredit) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown
    val iconColor = if (isCredit) Color(0xFF4CAF50) else Color(0xFFF44336)
    val amountPrefix = if (isCredit) "+" else "-"

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.description.ifEmpty { "Transaction" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = transaction.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix${formatCurrency(transaction.amount, currencyCode)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
                Text(
                    text = formatDate(transaction.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatCurrency(amount: Double, currencyCode: String): String {
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = java.util.Currency.getInstance(currencyCode)
    }
    return format.format(amount)
}

private fun formatCurrencyCompact(amount: Double, currencyCode: String): String {
    val symbol = java.util.Currency.getInstance(currencyCode).getSymbol(Locale.getDefault())
    val absAmount = Math.abs(amount)
    val sign = if (amount < 0) "-" else ""
    return when {
        absAmount >= 1_000_000_000_000 -> String.format("%s%s%.1fT", sign, symbol, absAmount / 1_000_000_000_000)
        absAmount >= 1_000_000_000 -> String.format("%s%s%.1fB", sign, symbol, absAmount / 1_000_000_000)
        absAmount >= 1_000_000 -> String.format("%s%s%.1fM", sign, symbol, absAmount / 1_000_000)
        absAmount >= 1_000 -> String.format("%s%s%.1fK", sign, symbol, absAmount / 1_000)
        else -> formatCurrency(amount, currencyCode)
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
