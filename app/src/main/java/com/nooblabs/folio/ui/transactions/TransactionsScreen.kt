package com.nooblabs.folio.ui.transactions

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
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooblabs.folio.domain.model.Transaction
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private val categories = listOf(
    "Food & Dining",
    "Shopping",
    "Transportation",
    "Housing & Utilities",
    "Salary",
    "Entertainment",
    "Investments",
    "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(viewModel: TransactionsViewModel, onNavigateToSettings: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var editingTx by remember { mutableStateOf<Transaction?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Filtering states
    var selectedTypeFilter by remember { mutableStateOf("ALL") } // "ALL", "CREDIT", "DEBIT"
    var selectedSourceFilterId by remember { mutableStateOf<Int?>(null) }
    var selectedSourceFilterType by remember { mutableStateOf<String?>(null) } // "BANK", "CARD"
    var showFilterMenu by remember { mutableStateOf(false) }

    val filteredTransactions = uiState.transactions.filter { uiModel ->
        val tx = uiModel.transaction
        val matchesType = selectedTypeFilter == "ALL" || tx.type == selectedTypeFilter
        val matchesSource = selectedSourceFilterId == null || 
                (tx.sourceId == selectedSourceFilterId && tx.sourceType == selectedSourceFilterType)
        matchesType && matchesSource
    }

    // Grouping by Date
    val groupedTransactions = filteredTransactions.groupBy { uiModel ->
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date(uiModel.transaction.date))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    // Currency Selector
                    var expandedCurrency by remember { mutableStateOf(false) }
                    val currencies = com.nooblabs.folio.domain.util.CurrencyConverter.supportedCurrencies
                    Box {
                        TextButton(onClick = { expandedCurrency = true }) {
                            Text("${uiState.globalCurrency} ▼", color = MaterialTheme.colorScheme.onBackground)
                        }
                        DropdownMenu(
                            expanded = expandedCurrency,
                            onDismissRequest = { expandedCurrency = false }
                        ) {
                            currencies.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency) },
                                    onClick = {
                                        viewModel.setGlobalCurrency(currency)
                                        expandedCurrency = false
                                    }
                                )
                            }
                        }
                    }

                    // Filter Menu
                    IconButton(onClick = { showFilterMenu = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filter")
                    }

                    // Settings Button
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTx = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Active Filters Row
                if (selectedTypeFilter != "ALL" || selectedSourceFilterId != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filters:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        if (selectedTypeFilter != "ALL") {
                            FilterChip(
                                selected = true,
                                onClick = { selectedTypeFilter = "ALL" },
                                label = { Text(selectedTypeFilter) }
                            )
                        }
                        if (selectedSourceFilterId != null) {
                            val sourceName = uiState.sources.find {
                                it.id == selectedSourceFilterId && it.type == selectedSourceFilterType
                            }?.name ?: "Source"
                            FilterChip(
                                selected = true,
                                onClick = {
                                    selectedSourceFilterId = null
                                    selectedSourceFilterType = null
                                },
                                label = { Text(sourceName) }
                            )
                        }
                    }
                }

                if (filteredTransactions.isEmpty()) {
                    EmptyTransactionsState()
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedTransactions.forEach { (dateStr, list) ->
                            val headerText = getHeaderDateString(dateStr)

                            item(key = "header_$dateStr") {
                                Text(
                                    text = headerText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            items(list, key = { it.transaction.id }) { uiModel ->
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
                                    TransactionItemCard(
                                        uiModel = uiModel,
                                        onClick = {
                                            editingTx = uiModel.transaction
                                            showSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Filter Dropdown Dialog
    if (showFilterMenu) {
        AlertDialog(
            onDismissRequest = { showFilterMenu = false },
            confirmButton = {
                TextButton(onClick = { showFilterMenu = false }) {
                    Text("Close")
                }
            },
            title = { Text("Filter Transactions", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Type Filter
                    Column {
                        Text("Transaction Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("ALL", "CREDIT", "DEBIT").forEach { type ->
                                FilterChip(
                                    selected = selectedTypeFilter == type,
                                    onClick = { selectedTypeFilter = type },
                                    label = { Text(type) }
                                )
                            }
                        }
                    }

                    // Source Filter
                    Column {
                        Text("Account Source", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            var showSourceDrop by remember { mutableStateOf(false) }
                            val filterSourceName = uiState.sources.find {
                                it.id == selectedSourceFilterId && it.type == selectedSourceFilterType
                            }?.name ?: "All Sources"
                            
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showSourceDrop = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(filterSourceName)
                                }
                                DropdownMenu(
                                    expanded = showSourceDrop,
                                    onDismissRequest = { showSourceDrop = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Sources") },
                                        onClick = {
                                            selectedSourceFilterId = null
                                            selectedSourceFilterType = null
                                            showSourceDrop = false
                                        }
                                    )
                                    uiState.sources.forEach { source ->
                                        DropdownMenuItem(
                                            text = { Text("${source.name} (${source.type})") },
                                            onClick = {
                                                selectedSourceFilterId = source.id
                                                selectedSourceFilterType = source.type
                                                showSourceDrop = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    if (showSheet) {
        TransactionFormBottomSheet(
            txToEdit = editingTx,
            sources = uiState.sources,
            onDismiss = { showSheet = false },
            onSave = { amount, date, description, type, category, sourceId, sourceType, currency ->
                if (editingTx != null) {
                    viewModel.updateTransaction(
                        editingTx!!.id,
                        amount,
                        date,
                        description,
                        type,
                        category,
                        sourceId,
                        sourceType,
                        currency,
                        editingTx!!
                    )
                } else {
                    viewModel.addTransaction(amount, date, description, type, category, sourceId, sourceType, currency)
                }
                showSheet = false
            },
            onDelete = { tx ->
                viewModel.deleteTransaction(tx)
                showSheet = false
            }
        )
    }
}

@Composable
fun EmptyTransactionsState() {
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
                imageVector = Icons.Filled.List,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Transactions Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to record a new transaction, or adjust your active filters.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun TransactionItemCard(uiModel: TransactionUiModel, onClick: () -> Unit) {
    val tx = uiModel.transaction
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = Currency.getInstance(uiModel.globalCurrency)
    }

    val isCredit = tx.type == "CREDIT"
    val prefix = if (isCredit) "+" else "-"
    val amountColor = if (isCredit) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onBackground
    val typeIcon = if (isCredit) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    val iconColor = if (isCredit) Color(0xFF4CAF50) else Color(0xFFF44336)
    val iconBgColor = if (isCredit) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFFF44336).copy(alpha = 0.15f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
        )
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
                    .background(iconBgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tx.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiModel.sourceName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$prefix${format.format(uiModel.convertedAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = amountColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(tx.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFormBottomSheet(
    txToEdit: Transaction?,
    sources: List<TransactionSource>,
    onDismiss: () -> Unit,
    onSave: (Double, Long, String, String, String, Int, String, String) -> Unit,
    onDelete: (Transaction) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var description by remember { mutableStateOf(txToEdit?.description ?: "") }
    var amountText by remember { 
        mutableStateOf(txToEdit?.amount?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "") 
    }
    var txType by remember { mutableStateOf(txToEdit?.type ?: "DEBIT") }
    var category by remember { mutableStateOf(txToEdit?.category ?: "Food & Dining") }
    var selectedSource by remember { 
        mutableStateOf(sources.find { it.id == txToEdit?.sourceId && it.type == txToEdit?.sourceType } ?: sources.firstOrNull()) 
    }
    var transactionDate by remember { mutableStateOf(txToEdit?.date ?: System.currentTimeMillis()) }
    var currency by remember { mutableStateOf(txToEdit?.currency ?: selectedSource?.currency ?: "USD") }

    LaunchedEffect(selectedSource) {
        if (txToEdit == null && selectedSource != null) {
            currency = selectedSource!!.currency
        }
    }

    val isEditMode = txToEdit != null
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateForm = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) "Edit Transaction" else "Add Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isEditMode) {
                    IconButton(onClick = { onDelete(txToEdit!!) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Debit/Credit Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                listOf("DEBIT", "CREDIT").forEach { type ->
                    val selected = txType == type
                    Button(
                        onClick = { txType = type },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) {
                                if (type == "CREDIT") Color(0xFF4CAF50) else Color(0xFFF44336)
                            } else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(if (type == "DEBIT") "Debit / Expense" else "Credit / Income", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                placeholder = { Text("e.g. Starbucks, Salary payment") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount ($currency)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Category Select
                var showCategoryDrop by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showCategoryDrop = true }
                    )
                    DropdownMenu(
                        expanded = showCategoryDrop,
                        onDismissRequest = { showCategoryDrop = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    showCategoryDrop = false
                                }
                            )
                        }
                    }
                }

                // Source Select
                var showSourceDrop by remember { mutableStateOf(false) }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedSource?.name ?: "No source",
                        onValueChange = {},
                        label = { Text("Source") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        enabled = false,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showSourceDrop = true }
                    )
                    DropdownMenu(
                        expanded = showSourceDrop,
                        onDismissRequest = { showSourceDrop = false }
                    ) {
                        sources.forEach { src ->
                            DropdownMenuItem(
                                text = { Text("${src.name} (${src.type})") },
                                onClick = {
                                    selectedSource = src
                                    showSourceDrop = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showCurrencyDrop by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = currency,
                    onValueChange = {},
                    label = { Text("Currency") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showCurrencyDrop = true }
                )
                DropdownMenu(
                    expanded = showCurrencyDrop,
                    onDismissRequest = { showCurrencyDrop = false }
                ) {
                    com.nooblabs.folio.domain.util.CurrencyConverter.supportedCurrencies.forEach { curr ->
                        DropdownMenuItem(
                            text = { Text(curr) },
                            onClick = {
                                currency = curr
                                showCurrencyDrop = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date Picker Box
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = dateForm.format(Date(transactionDate)),
                    onValueChange = {},
                    label = { Text("Transaction Date & Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable {
                            val calendar = Calendar.getInstance().apply { timeInMillis = transactionDate }
                            android.app.DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    android.app.TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val newCal = Calendar.getInstance().apply {
                                                set(year, month, day, hour, minute)
                                            }
                                            transactionDate = newCal.timeInMillis
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val source = selectedSource
                    if (description.isNotBlank() && amount > 0 && source != null) {
                        onSave(amount, transactionDate, description, txType, category, source.id, source.type, currency)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                enabled = description.isNotBlank() && amountText.isNotBlank() && selectedSource != null
            ) {
                Text(
                    text = if (isEditMode) "Update Transaction" else "Save Transaction",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun getHeaderDateString(dateStr: String): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val date = sdf.parse(dateStr) ?: return dateStr
    
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
    
    val target = Calendar.getInstance().apply { time = date }
    
    return when {
        isSameDay(today, target) -> "Today"
        isSameDay(yesterday, target) -> "Yesterday"
        else -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
           cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
