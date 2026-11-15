package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Building
import com.example.data.model.Flat
import com.example.data.model.Tenant
import com.example.i18n.AppLanguage
import com.example.i18n.LocalLanguage
import com.example.i18n.LocalStrings
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.AdminDataSnapshot
import com.example.util.FormatUtils

@Composable
fun AdminTenantsScreen(
    state: AdminDataSnapshot,
    onSaveAndAssignTenant: (Tenant, Flat) -> Unit,
    onToggleStatus: (Tenant) -> Unit,
    onVacateTenant: (Tenant) -> Unit,
    onDeleteTenant: (Tenant) -> Unit = {},
    onSendPersonalMessage: (Tenant, String, String, String) -> Unit,
    onSendAnnouncement: (String, String, String, Building?) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current

    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") }

    var showAddTenantDialog by remember { mutableStateOf(false) }
    var editingTenant by remember { mutableStateOf<Tenant?>(null) }
    var messageTargetTenant by remember { mutableStateOf<Tenant?>(null) }
    var historyTargetTenant by remember { mutableStateOf<Tenant?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var pendingDeleteTenant by remember { mutableStateOf<Tenant?>(null) }

    val filteredTenants = state.tenants.filter { t ->
        val matchesStatus = filterStatus == "all" || t.status == filterStatus
        val matchesSearch = searchQuery.isBlank() ||
            t.name.contains(searchQuery, ignoreCase = true) ||
            t.phone.contains(searchQuery, ignoreCase = true) ||
            t.flatNumber.contains(searchQuery, ignoreCase = true) ||
            t.buildingName.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesSearch
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingTenant = null
                    showAddTenantDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_tenant")
            ) {
                Icon(Icons.Filled.Add, contentDescription = strings.addTenant)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.navTenants} (${FormatUtils.formatInt(state.tenants.size, language)})",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    OutlinedButton(
                        onClick = { showAnnouncementDialog = true },
                        modifier = Modifier.testTag("send_announcement_button")
                    ) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(strings.sendAnnouncement)
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(if (language == AppLanguage.BN) "নাম, ফ্ল্যাট বা মোবাইল দিয়ে খুঁজুন..." else "Search name, flat or phone...")
                    },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("tenant_search_input")
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("all" to "All", "active" to strings.active, "inactive" to strings.inactive, "vacated" to strings.vacated).forEach { (key, label) ->
                        FilterChip(
                            selected = filterStatus == key,
                            onClick = { filterStatus = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            items(filteredTenants, key = { it.id }) { tenant ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column {
                                    Text(
                                        text = tenant.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${tenant.buildingName} • Flat ${tenant.flatNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(tenant.status)
                                IconButton(
                                    onClick = { pendingDeleteTenant = tenant },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_tenant_${tenant.id}")
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = strings.delete,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                Text(tenant.phone, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(tenant.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.active}/${strings.inactive}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Switch(
                                checked = tenant.status == "active",
                                onCheckedChange = { onToggleStatus(tenant) },
                                enabled = tenant.status != "vacated"
                            )
                        }

                        HorizontalDivider()

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { messageTargetTenant = tenant },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.sendMessage, maxLines = 1)
                            }
                            OutlinedButton(
                                onClick = { historyTargetTenant = tenant },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.paymentHistory, maxLines = 1)
                            }
                        }
                        if (tenant.status != "vacated") {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingTenant = tenant
                                        showAddTenantDialog = true
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(strings.transferFlat, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { onVacateTenant(tenant) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(strings.vacateTenant, color = MaterialTheme.colorScheme.error, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }

    if (showAddTenantDialog) {
        TenantAssignDialog(
            initial = editingTenant,
            flats = state.flats,
            onDismiss = { showAddTenantDialog = false },
            onSave = { tenant, flat ->
                showAddTenantDialog = false
                onSaveAndAssignTenant(tenant, flat)
            }
        )
    }

    messageTargetTenant?.let { tenant ->
        SendMessageDialog(
            tenant = tenant,
            onDismiss = { messageTargetTenant = null },
            onSend = { title, body, type ->
                messageTargetTenant = null
                onSendPersonalMessage(tenant, title, body, type)
            }
        )
    }

    historyTargetTenant?.let { tenant ->
        val tenantPayments = state.payments.filter { it.tenantId == tenant.id }
        AlertDialog(
            onDismissRequest = { historyTargetTenant = null },
            title = { Text("${tenant.name} - ${strings.paymentHistory}") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tenantPayments.isEmpty()) {
                        Text(if (language == AppLanguage.BN) "কোনো পেমেন্ট রেকর্ড নেই।" else "No payment records yet.")
                    } else {
                        tenantPayments.forEach { p ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("${p.paymentDate} • ${p.method.uppercase()}", style = MaterialTheme.typography.bodySmall)
                                    Text(p.transactionId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(FormatUtils.formatBdt(p.amount, language), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    StatusBadge(p.verificationStatus)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { historyTargetTenant = null }) { Text(strings.close) }
            }
        )
    }

    if (showAnnouncementDialog) {
        BroadcastAnnouncementDialog(
            buildings = state.buildings,
            onDismiss = { showAnnouncementDialog = false },
            onSend = { title, body, cat, bldg ->
                showAnnouncementDialog = false
                onSendAnnouncement(title, body, cat, bldg)
            }
        )
    }

    pendingDeleteTenant?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeleteTenant = null },
            title = { Text(strings.delete) },
            text = {
                Text(
                    if (language == AppLanguage.BN) {
                        "আপনি কি নিশ্চিতভাবে ভাড়াটিয়া \"${target.name}\" কে মুছে ফেলতে চান?"
                    } else {
                        "Are you sure you want to delete tenant \"${target.name}\"?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeleteTenant = null
                        onDeleteTenant(target)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_tenant_btn")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTenant = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TenantAssignDialog(
    initial: Tenant?,
    flats: List<Flat>,
    onDismiss: () -> Unit,
    onSave: (Tenant, Flat) -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var nid by remember { mutableStateOf(initial?.nidNumber.orEmpty()) }
    var moveIn by remember { mutableStateOf(initial?.moveInDate?.ifBlank { FormatUtils.todayDdMmYyyy() } ?: FormatUtils.todayDdMmYyyy()) }

    val availableFlats = flats.filter { !it.isOccupied || it.id == initial?.flatId }
    var selectedFlat by remember { mutableStateOf(availableFlats.firstOrNull { it.id == initial?.flatId } ?: availableFlats.firstOrNull()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addTenant else strings.editTenant) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.tenantName) },
                    modifier = Modifier.fillMaxWidth().testTag("tenant_name_input")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(strings.tenantEmail) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth().testTag("tenant_email_input")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.tenantPhone) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("tenant_phone_input")
                )
                OutlinedTextField(
                    value = nid,
                    onValueChange = { nid = it },
                    label = { Text(strings.nidNumber) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = moveIn,
                    onValueChange = { moveIn = it },
                    label = { Text(strings.moveInDate) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (availableFlats.isEmpty()) {
                    Text(
                        text = "No vacant flats available. Add a flat in Buildings tab first.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = selectedFlat?.let { "${it.buildingName} - ${it.flatNumber}" } ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(strings.assignFlat) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            availableFlats.forEach { flat ->
                                DropdownMenuItem(
                                    text = { Text("${flat.buildingName} - ${flat.flatNumber}") },
                                    onClick = {
                                        selectedFlat = flat
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val flat = selectedFlat ?: return@Button
                    onSave(
                        (initial ?: Tenant()).copy(
                            name = name,
                            email = email,
                            phone = phone,
                            nidNumber = nid,
                            moveInDate = moveIn,
                            status = initial?.status?.takeIf { it != "vacated" } ?: "active"
                        ),
                        flat
                    )
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && email.isNotBlank() && selectedFlat != null,
                modifier = Modifier.testTag("save_tenant_button")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun SendMessageDialog(
    tenant: Tenant,
    onDismiss: () -> Unit,
    onSend: (String, String, String) -> Unit
) {
    val strings = LocalStrings.current
    var title by remember { mutableStateOf("Rent Reminder - ${tenant.flatNumber}") }
    var body by remember { mutableStateOf("Dear ${tenant.name}, please clear your monthly rent bill at your earliest convenience.") }
    var type by remember { mutableStateOf("reminder") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.sendMessage}: ${tenant.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("personal", "reminder", "maintenance").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.messageTitle) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(strings.messageBody) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, body, type) },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text(strings.sendMessage) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun BroadcastAnnouncementDialog(
    buildings: List<Building>,
    onDismiss: () -> Unit,
    onSend: (String, String, String, Building?) -> Unit
) {
    val strings = LocalStrings.current
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("notice") }
    var targetBuilding by remember { mutableStateOf<Building?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.sendAnnouncement) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("notice", "emergency", "maintenance").forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = targetBuilding == null,
                        onClick = { targetBuilding = null },
                        label = { Text("All Buildings") }
                    )
                    buildings.take(3).forEach { b ->
                        FilterChip(
                            selected = targetBuilding?.id == b.id,
                            onClick = { targetBuilding = b },
                            label = { Text(b.name.take(12)) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(strings.messageTitle) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text(strings.messageBody) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(title, body, category, targetBuilding) },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text(strings.sendAnnouncement) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
