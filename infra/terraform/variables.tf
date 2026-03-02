variable "environment" {
  description = "Deployment environment (prod, staging, dev)"
  type        = string
  default     = "prod"
}

variable "location" {
  description = "Azure region"
  type        = string
  default     = "eastus2"
}

variable "location_short" {
  description = "Short name used in resource naming"
  type        = string
  default     = "eus2"
}

variable "kubernetes_version" {
  description = "AKS Kubernetes version"
  type        = string
  default     = "1.31"
}

variable "eventhub_capacity" {
  description = "Event Hubs Premium capacity units"
  type        = number
  default     = 1
}

variable "flink_node_count_min" {
  type    = number
  default = 2
}

variable "flink_node_count_max" {
  type    = number
  default = 6
}

variable "app_node_count_min" {
  type    = number
  default = 2
}

variable "app_node_count_max" {
  type    = number
  default = 4
}

variable "alert_email" {
  description = "Email address for critical alerts"
  type        = string
  default     = "ops@example.com"
}

variable "slack_webhook_url" {
  description = "Slack webhook URL for Alertmanager"
  type        = string
  sensitive   = true
  default     = ""
}

locals {
  name_prefix = "${var.environment}-${var.location_short}-orders"
  tags = {
    environment = var.environment
    project     = "orders-dashboard"
    managed_by  = "terraform"
  }
}
