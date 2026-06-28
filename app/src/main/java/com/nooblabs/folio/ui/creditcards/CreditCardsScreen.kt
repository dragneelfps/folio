package com.nooblabs.folio.ui.creditcards

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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nooblabs.folio.domain.model.CreditCard
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(viewModel: CreditCardsViewModel, onNavigateToSettings: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }
    var editingCard by remember { mutableStateOf<CreditCard?>(null) }

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
                    editingCard = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Card")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.cards.isEmpty()) {
                EmptyCardsState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.cards, key = { it.card.id }) { uiModel ->
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
                            CreditCardItem(
                                uiModel = uiModel,
                                onClick = {
                                    editingCard = uiModel.card
                                    showSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSheet) {
        CardFormBottomSheet(
            cardToEdit = editingCard,
            globalCurrency = uiState.globalCurrency,
            onDismiss = { showSheet = false },
            onSave = { name, number, limit, outstanding, dueDate, expiry ->
                if (editingCard != null) {
                    viewModel.updateCreditCard(
                        editingCard!!.id,
                        name,
                        number,
                        limit,
                        outstanding,
                        dueDate,
                        expiry
                    )
                } else {
                    viewModel.addCreditCard(name, number, limit, outstanding, dueDate, expiry)
                }
                showSheet = false
            },
            onDelete = { card ->
                viewModel.deleteCreditCard(card)
                showSheet = false
            }
        )
    }
}

@Composable
fun EmptyCardsState() {
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
                imageVector = Icons.Filled.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Credit Cards",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Tap the + button to add your first credit card and keep track of limits and due dates.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun CreditCardItem(uiModel: CreditCardUiModel, onClick: () -> Unit) {
    val card = uiModel.card
    val format = NumberFormat.getCurrencyInstance(Locale.getDefault()).apply {
        currency = java.util.Currency.getInstance(uiModel.globalCurrency)
    }

    val utilizationPercent = if (card.creditLimit > 0) {
        (card.currentOutstanding / card.creditLimit) * 100
    } else 0.0

    val progressValue = (card.currentOutstanding / card.creditLimit).toFloat().coerceIn(0f, 1f)

    val utilizationColor = when {
        utilizationPercent >= 75.0 -> Color(0xFFF44336) // Red (Over utilized)
        utilizationPercent >= 50.0 -> Color(0xFFFF9800) // Orange/Yellow
        else -> Color(0xFF4CAF50) // Green
    }

    // Due date warning calculation
    val daysRemaining = ((card.dueDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val dueText = when {
        daysRemaining < 0 -> "Overdue by ${-daysRemaining} day(s)!"
        daysRemaining == 0 -> "Due today!"
        daysRemaining == 1 -> "Due tomorrow!"
        else -> "Due in $daysRemaining days"
    }

    val dueColor = when {
        daysRemaining <= 3 -> Color(0xFFF44336) // Red
        daysRemaining <= 7 -> Color(0xFFFF9800) // Orange
        else -> MaterialTheme.colorScheme.primary
    }

    // Premium card gradient
    val cardGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2C3E50),
            Color(0xFF0F2027),
            Color(0xFF203A43)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Virtual Card UI
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(cardGradient)
                    .padding(20.dp)
            ) {
                // Background design element
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .background(Color.White.copy(alpha = 0.05f), CircleShape)
                        .align(Alignment.BottomEnd)
                        .offset(x = 40.dp, y = 40.dp)
                )

                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = card.cardName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Filled.CreditCard,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Outstanding Balance",
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = format.format(uiModel.convertedOutstanding),
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = if (card.cardNumber.length >= 4) {
                                "•••• •••• •••• ${card.cardNumber.takeLast(4)}"
                            } else "•••• •••• •••• ••••",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodyMedium,
                            letterSpacing = 2.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "EXPIRY",
                                    color = Color.White.copy(alpha = 0.4f),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = card.expiry,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Utilization and Due Date details below card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Utilization: ${String.format("%.1f%%", utilizationPercent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.5f)
                    )
                    Text(
                        text = "Limit: ${format.format(uiModel.convertedLimit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(0.5f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progressValue },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = utilizationColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (daysRemaining <= 7) Icons.Filled.Warning else Icons.Filled.CreditCard,
                        contentDescription = null,
                        tint = dueColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$dueText (${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(card.dueDate))})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = dueColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardFormBottomSheet(
    cardToEdit: CreditCard?,
    globalCurrency: String,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Long, String) -> Unit,
    onDelete: (CreditCard) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var cardName by remember { mutableStateOf(cardToEdit?.cardName ?: "") }
    var cardNumber by remember { mutableStateOf(cardToEdit?.cardNumber ?: "") }
    var limitText by remember { 
        mutableStateOf(cardToEdit?.creditLimit?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "") 
    }
    var outstandingText by remember { 
        mutableStateOf(cardToEdit?.currentOutstanding?.let { java.math.BigDecimal.valueOf(it).stripTrailingZeros().toPlainString() } ?: "") 
    }
    var selectedCurrency by remember { mutableStateOf(globalCurrency) }
    var dueDate by remember { mutableStateOf(cardToEdit?.dueDate ?: System.currentTimeMillis()) }
    var expiry by remember { mutableStateOf(cardToEdit?.expiry ?: "") }

    val isEditMode = cardToEdit != null
    val context = androidx.compose.ui.platform.LocalContext.current
    val dateForm = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

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
                    text = if (isEditMode) "Edit Credit Card" else "Add Credit Card",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                if (isEditMode) {
                    IconButton(onClick = { onDelete(cardToEdit!!) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete Card",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text("Card Name (e.g. Sapphire Preferred)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) cardNumber = it },
                label = { Text("Card Number (Last 4 digits or all 16)") },
                placeholder = { Text("e.g. 1234") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(16.dp)
            )
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
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Limit") },
                    modifier = Modifier.weight(0.6f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = outstandingText,
                onValueChange = { outstandingText = it },
                label = { Text("Outstanding") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = expiry,
                onValueChange = { if (it.length <= 5) expiry = it },
                label = { Text("Expiry (MM/YY)") },
                placeholder = { Text("12/28") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date Selector
            OutlinedTextField(
                value = dateForm.format(Date(dueDate)),
                onValueChange = {},
                label = { Text("Payment Due Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val calendar = Calendar.getInstance().apply { timeInMillis = dueDate }
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val newCal = Calendar.getInstance().apply {
                                    set(year, month, day, 0, 0, 0)
                                }
                                dueDate = newCal.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
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
                    val limit = limitText.toDoubleOrNull() ?: 0.0
                    val outstanding = outstandingText.toDoubleOrNull() ?: 0.0
                    val limitInUsd = com.nooblabs.folio.domain.util.CurrencyConverter
                        .convert(limit, selectedCurrency, "USD")
                    val outstandingInUsd = com.nooblabs.folio.domain.util.CurrencyConverter
                        .convert(outstanding, selectedCurrency, "USD")
                    if (cardName.isNotBlank() && limit > 0) {
                        onSave(cardName, cardNumber, limitInUsd, outstandingInUsd, dueDate, expiry)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                enabled = cardName.isNotBlank() && limitText.isNotBlank() && expiry.isNotBlank()
            ) {
                Text(
                    text = if (isEditMode) "Update Card" else "Save Card",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
