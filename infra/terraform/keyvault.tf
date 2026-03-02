data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "main" {
  name                        = "${local.name_prefix}-kv"
  location                    = var.location
  resource_group_name         = azurerm_resource_group.main.name
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  sku_name                    = "standard"
  soft_delete_retention_days  = 7
  purge_protection_enabled    = true
  rbac_authorization_enabled  = true

  network_acls {
    default_action = "Deny"
    bypass         = "AzureServices"
    virtual_network_subnet_ids = [azurerm_subnet.aks.id]
  }

  tags = local.tags
}

# Allow AKS system-assigned identity to read secrets
resource "azurerm_role_assignment" "aks_keyvault_secrets" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_kubernetes_cluster.main.identity[0].principal_id
}

# Allow AKS kubelet identity to read secrets (needed by CSI driver)
resource "azurerm_role_assignment" "aks_kubelet_keyvault_secrets" {
  scope                = azurerm_key_vault.main.id
  role_definition_name = "Key Vault Secrets User"
  principal_id         = azurerm_kubernetes_cluster.main.kubelet_identity[0].object_id
}

# Store Event Hubs connection string as a secret
resource "azurerm_key_vault_secret" "eventhub_conn_string" {
  name         = "eventhub-conn-string"
  value        = azurerm_eventhub_namespace_authorization_rule.send_listen.primary_connection_string
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.aks_keyvault_secrets]
}

resource "azurerm_key_vault_secret" "eventhub_namespace" {
  name         = "eventhub-namespace"
  value        = azurerm_eventhub_namespace.main.name
  key_vault_id = azurerm_key_vault.main.id

  depends_on = [azurerm_role_assignment.aks_keyvault_secrets]
}
