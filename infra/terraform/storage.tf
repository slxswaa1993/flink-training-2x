resource "azurerm_storage_account" "flink_state" {
  name                     = "${replace(local.name_prefix, "-", "")}storage"
  resource_group_name      = azurerm_resource_group.main.name
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"
  account_kind             = "StorageV2"
  is_hns_enabled           = true  # ADLS Gen2 for ABFS

  blob_properties {
    delete_retention_policy {
      days = 7
    }
  }

  tags = local.tags
}

resource "azurerm_storage_container" "flink_state" {
  name                  = "flink-state"
  storage_account_name  = azurerm_storage_account.flink_state.name
  container_access_type = "private"
}

resource "azurerm_storage_container" "flink_savepoints" {
  name                  = "flink-savepoints"
  storage_account_name  = azurerm_storage_account.flink_state.name
  container_access_type = "private"
}

# Grant AKS managed identity access to storage
resource "azurerm_role_assignment" "aks_storage_blob" {
  scope                = azurerm_storage_account.flink_state.id
  role_definition_name = "Storage Blob Data Contributor"
  principal_id         = azurerm_kubernetes_cluster.main.identity[0].principal_id
}
