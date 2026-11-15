package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
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
fun AdminBuildingsScreen(
    state: AdminDataSnapshot,
    onSaveBuilding: (Building) -> Unit,
    onDeleteBuilding: (Building) -> Unit,
    onSaveFlat: (Flat) -> Unit,
    onDeleteFlat: (Flat) -> Unit = {},
    onAssignTenantToFlat: (Tenant, Flat) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current

    var selectedBuildingId by remember { mutableStateOf<String?>(null) }
    val selectedBuilding = state.buildings.firstOrNull { it.id == selectedBuildingId }

    var editingBuilding by remember { mutableStateOf<Building?>(null) }
    var showBuildingDialog by remember { mutableStateOf(false) }

    var editingFlat by remember { mutableStateOf<Flat?>(null) }
    var showFlatDialog by remember { mutableStateOf(false) }

    var assignFlatTarget by remember { mutableStateOf<Flat?>(null) }
    var pendingDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    if (selectedBuilding != null) {
        BackHandler { selectedBuildingId = null }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedBuilding == null) {
                        editingBuilding = null
                        showBuildingDialog = true
                    } else {
                        editingFlat = null
                        showFlatDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag(
                    if (selectedBuilding == null) "fab_add_building" else "fab_add_flat"
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = if (selectedBuilding == null) strings.addBuilding else strings.addFlat
                )
            }
        }
    ) { innerPadding ->
        if (selectedBuilding == null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "${strings.navBuildings} (${FormatUtils.formatInt(state.buildings.size, language)})",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (state.buildings.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.img_empty_buildings),
                                    contentDescription = "No Buildings",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = strings.noBuildingsYet,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(state.buildings, key = { it.id }) { building ->
                        val bFlats = state.flats.filter { it.buildingId == building.id }
                        val bOccupied = bFlats.count { it.isOccupied }
                        val bDue = bFlats.sumOf { it.remainingDue }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBuildingId = building.id }
                                .testTag("building_card_${building.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Apartment,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Column {
                                            Text(
                                                text = building.name,
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = building.address,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingBuilding = building
                                                showBuildingDialog = true
                                            }
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = strings.editBuilding)
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingDelete = building.name to { onDeleteBuilding(building) }
                                            },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("delete_building_${building.id}")
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = strings.deleteBuilding,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${strings.floorsCount}: ${FormatUtils.formatInt(building.floorsCount, language)} | ${strings.totalFlats}: ${FormatUtils.formatInt(bFlats.size, language)}/${FormatUtils.formatInt(building.totalFlats, language)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${strings.occupied}: ${FormatUtils.formatInt(bOccupied, language)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (bDue > 0) {
                                    Text(
                                        text = "${strings.remainingDue}: ${FormatUtils.formatBdt(bDue, language)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        } else {
            val buildingFlats = state.flats.filter { it.buildingId == selectedBuilding.id }
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { selectedBuildingId = null },
                            modifier = Modifier.testTag("back_to_buildings_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedBuilding.name,
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "${selectedBuilding.address} • ${selectedBuilding.contactInfo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (buildingFlats.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(strings.noFlatsYet, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = {
                                    editingFlat = null
                                    showFlatDialog = true
                                }) {
                                    Text(strings.addFlat)
                                }
                            }
                        }
                    }
                } else {
                    items(buildingFlats, key = { it.id }) { flat ->
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
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Filled.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "Flat ${flat.flatNumber} (Floor ${flat.floorNumber})",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    StatusBadge(flat.paymentStatus)
                                }

                                if (flat.isOccupied) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${flat.tenantName} (${flat.tenantPhone})",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = strings.vacant,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                HorizontalDivider()

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${strings.totalMonthlyBill}: ${FormatUtils.formatBdt(flat.totalMonthlyBill, language)}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "${strings.remainingDue}: ${FormatUtils.formatBdt(flat.remainingDue, language)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (flat.remainingDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            editingFlat = flat
                                            showFlatDialog = true
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(strings.edit)
                                    }
                                    if (!flat.isOccupied) {
                                        Button(
                                            onClick = { assignFlatTarget = flat },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(strings.addTenant)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            pendingDelete = "Flat ${flat.flatNumber}" to { onDeleteFlat(flat) }
                                        },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("delete_flat_${flat.id}")
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = strings.delete,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showBuildingDialog) {
        BuildingFormDialog(
            initial = editingBuilding,
            onDismiss = { showBuildingDialog = false },
            onSave = {
                showBuildingDialog = false
                onSaveBuilding(it)
            }
        )
    }

    if (showFlatDialog && selectedBuilding != null) {
        FlatFormDialog(
            building = selectedBuilding,
            initial = editingFlat,
            onDismiss = { showFlatDialog = false },
            onSave = {
                showFlatDialog = false
                onSaveFlat(it)
            }
        )
    }

    assignFlatTarget?.let { targetFlat ->
        QuickAssignTenantDialog(
            flat = targetFlat,
            onDismiss = { assignFlatTarget = null },
            onAssign = { tenant ->
                assignFlatTarget = null
                onAssignTenantToFlat(tenant, targetFlat)
            }
        )
    }

    pendingDelete?.let { (label, action) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(strings.delete) },
            text = {
                Text(
                    if (language == AppLanguage.BN) {
                        "আপনি কি নিশ্চিতভাবে \"$label\" মুছে ফেলতে চান?"
                    } else {
                        "Are you sure you want to delete \"$label\"?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDelete = null
                        action()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_building_or_flat_btn")
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun BuildingFormDialog(
    initial: Building?,
    onDismiss: () -> Unit,
    onSave: (Building) -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var floors by remember { mutableStateOf(initial?.floorsCount?.toString() ?: "6") }
    var totalFlats by remember { mutableStateOf(initial?.totalFlats?.toString() ?: "12") }
    var contact by remember { mutableStateOf(initial?.contactInfo.orEmpty()) }
    var notes by remember { mutableStateOf(initial?.notes.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addBuilding else strings.editBuilding) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.buildingName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("building_name_input")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(strings.buildingAddress) },
                    modifier = Modifier.fillMaxWidth().testTag("building_address_input")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = floors,
                        onValueChange = { floors = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.floorsCount) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalFlats,
                        onValueChange = { totalFlats = it.filter { ch -> ch.isDigit() } },
                        label = { Text(strings.totalFlats) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text(strings.contactInfo) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(strings.notes) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && address.isNotBlank()) {
                        onSave(
                            (initial ?: Building()).copy(
                                name = name,
                                address = address,
                                floorsCount = floors.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                totalFlats = totalFlats.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                contactInfo = contact,
                                notes = notes
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && address.isNotBlank(),
                modifier = Modifier.testTag("save_building_button")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun FlatFormDialog(
    building: Building,
    initial: Flat?,
    onDismiss: () -> Unit,
    onSave: (Flat) -> Unit
) {
    val strings = LocalStrings.current
    val language = LocalLanguage.current
    var flatNumber by remember { mutableStateOf(initial?.flatNumber ?: "2A") }
    var floorNumber by remember { mutableStateOf(initial?.floorNumber?.toString() ?: "2") }
    var rent by remember { mutableStateOf(initial?.monthlyRent?.toInt()?.toString() ?: "23000") }
    var service by remember { mutableStateOf(initial?.serviceCharge?.toInt()?.toString() ?: "3500") }
    var elec by remember { mutableStateOf(initial?.electricityBill?.toInt()?.toString() ?: "0") }
    var water by remember { mutableStateOf(initial?.waterBill?.toInt()?.toString() ?: "0") }
    var gas by remember { mutableStateOf(initial?.gasBill?.toInt()?.toString() ?: "1080") }
    var other by remember { mutableStateOf(initial?.otherCharges?.toInt()?.toString() ?: "0") }

    val previewTotal = (rent.toDoubleOrNull() ?: 0.0) +
        (service.toDoubleOrNull() ?: 0.0) +
        (elec.toDoubleOrNull() ?: 0.0) +
        (water.toDoubleOrNull() ?: 0.0) +
        (gas.toDoubleOrNull() ?: 0.0) +
        (other.toDoubleOrNull() ?: 0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) strings.addFlat else strings.editFlat) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = flatNumber,
                        onValueChange = { flatNumber = it },
                        label = { Text(strings.flatNumber) },
                        modifier = Modifier.weight(1f).testTag("flat_number_input")
                    )
                    OutlinedTextField(
                        value = floorNumber,
                        onValueChange = { floorNumber = it.filter { c -> c.isDigit() } },
                        label = { Text(strings.floorNumber) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rent,
                        onValueChange = { rent = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.monthlyRent) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = service,
                        onValueChange = { service = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.serviceCharge) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = elec,
                        onValueChange = { elec = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.electricityBill) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = water,
                        onValueChange = { water = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.waterBill) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = gas,
                        onValueChange = { gas = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.gasBill) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = other,
                        onValueChange = { other = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text(strings.otherCharges) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "${strings.totalMonthlyBill}: ${FormatUtils.formatBdt(previewTotal, language)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (flatNumber.isNotBlank()) {
                        onSave(
                            (initial ?: Flat()).copy(
                                buildingId = building.id,
                                buildingName = building.name,
                                flatNumber = flatNumber,
                                floorNumber = floorNumber.toIntOrNull() ?: 1,
                                monthlyRent = rent.toDoubleOrNull() ?: 0.0,
                                serviceCharge = service.toDoubleOrNull() ?: 0.0,
                                electricityBill = elec.toDoubleOrNull() ?: 0.0,
                                waterBill = water.toDoubleOrNull() ?: 0.0,
                                gasBill = gas.toDoubleOrNull() ?: 0.0,
                                otherCharges = other.toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                },
                enabled = flatNumber.isNotBlank(),
                modifier = Modifier.testTag("save_flat_button")
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}

@Composable
private fun QuickAssignTenantDialog(
    flat: Flat,
    onDismiss: () -> Unit,
    onAssign: (Tenant) -> Unit
) {
    val strings = LocalStrings.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var nid by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${strings.addTenant} (${flat.flatNumber})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(strings.tenantName) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(strings.tenantEmail) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text(strings.tenantPhone) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nid,
                    onValueChange = { nid = it },
                    label = { Text(strings.nidNumber) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onAssign(
                        Tenant(
                            name = name,
                            email = email,
                            phone = phone,
                            nidNumber = nid,
                            status = "active",
                            moveInDate = FormatUtils.todayDdMmYyyy()
                        )
                    )
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && email.isNotBlank()
            ) { Text(strings.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        }
    )
}
