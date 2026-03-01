resource "azurerm_eventhub_namespace" "main" {
  name                = "${replace(local.name_prefix, "-", "")}ehns"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  sku                 = "Premium"
  capacity            = var.eventhub_capacity

  public_network_access_enabled = false

  tags = local.tags
}

locals {
  event_hubs = [
    "orders-raw",
    "orders-by-region",
    "top-products",
    "revenue-by-category",
    "high-value-alerts",
  ]
}

resource "azurerm_eventhub" "hubs" {
  for_each = toset(local.event_hubs)

  name                = each.key
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 4
  message_retention   = 7
}

resource "azurerm_eventhub_consumer_group" "flink" {
  for_each = toset(local.event_hubs)

  name                = "flink-consumer"
  namespace_name      = azurerm_eventhub_namespace.main.name
  eventhub_name       = azurerm_eventhub.hubs[each.key].name
  resource_group_name = azurerm_resource_group.main.name
}

resource "azurerm_eventhub_consumer_group" "backend" {
  for_each = toset(["orders-by-region", "top-products", "revenue-by-category", "high-value-alerts"])

  name                = "backend-consumer"
  namespace_name      = azurerm_eventhub_namespace.main.name
  eventhub_name       = azurerm_eventhub.hubs[each.key].name
  resource_group_name = azurerm_resource_group.main.name
}

resource "azurerm_eventhub_namespace_authorization_rule" "send_listen" {
  name                = "orders-dashboard-rule"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  listen              = true
  send                = true
  manage              = false
}

resource "azurerm_private_endpoint" "eventhub" {
  name                = "${local.name_prefix}-eh-pe"
  location            = var.location
  resource_group_name = azurerm_resource_group.main.name
  subnet_id           = azurerm_subnet.private_endpoints.id

  private_service_connection {
    name                           = "${local.name_prefix}-eh-psc"
    private_connection_resource_id = azurerm_eventhub_namespace.main.id
    subresource_names              = ["namespace"]
    is_manual_connection           = false
  }

  tags = local.tags
}
