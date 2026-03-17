variable "project" {}
variable "environment" {}
variable "vpc_id" {}
variable "subnet_ids" { type = list(string) }
variable "instance_type"  { default = "kafka.t3.small" }
variable "kafka_version"  { default = "3.5.1" }
variable "broker_count"   { default = 2 }

resource "aws_msk_cluster" "main" {
  cluster_name           = "${var.project}-${var.environment}"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.broker_count

  broker_node_group_info {
    instance_type  = var.instance_type
    client_subnets = var.subnet_ids
    storage_info {
      ebs_storage_info { volume_size = 20 }
    }
  }

  encryption_info {
    encryption_in_transit { client_broker = "TLS_PLAINTEXT" }
  }
}

output "bootstrap_brokers" { value = aws_msk_cluster.main.bootstrap_brokers }
