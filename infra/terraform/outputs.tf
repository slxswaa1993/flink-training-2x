output "aks_cluster_name" {
  description = "AKS cluster name"
  value       = azurerm_kubernetes_cluster.main.name
}

output "aks_resource_group" {
  description = "AKS resource group"
  value       = azurerm_resource_group.main.name
}

output "acr_login_server" {
  description = "ACR login server URL"
  value       = azurerm_container_registry.main.login_server
}

output "eventhub_namespace" {
  description = "Event Hubs namespace name"
  value       = azurerm_eventhub_namespace.main.name
}

output "eventhub_bootstrap_servers" {
  description = "Kafka-protocol bootstrap servers"
  value       = "${azurerm_eventhub_namespace.main.name}.servicebus.windows.net:9093"
}

output "flink_checkpoint_dir" {
  description = "ABFS URI for Flink checkpoints"
  value       = "abfs://flink-state@${azurerm_storage_account.flink_state.name}.dfs.core.windows.net/checkpoints"
}

output "key_vault_name" {
  description = "Key Vault name"
  value       = azurerm_key_vault.main.name
}

output "log_analytics_workspace_id" {
  description = "Log Analytics workspace ID"
  value       = azurerm_log_analytics_workspace.main.id
}

output "kubeconfig_command" {
  description = "Command to configure kubectl"
  value       = "az aks get-credentials --resource-group ${azurerm_resource_group.main.name} --name ${azurerm_kubernetes_cluster.main.name}"
}
