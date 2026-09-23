variable "project" {}
variable "environment" {}
variable "vpc_id" {}
variable "vpc_cidr" {}
variable "subnet_ids" { type = list(string) }
variable "instance_type" { default = "kafka.t3.small" }
variable "kafka_version" { default = "3.5.1" }
variable "broker_count" { default = 2 }
variable "scram_secret_arns" {
  type    = list(string)
  default = []
}

resource "aws_security_group" "msk" {
  name        = "${var.project}-${var.environment}-msk"
  description = "Allow Kafka clients inside the VPC"
  vpc_id      = var.vpc_id

  ingress {
    from_port   = 9092
    to_port     = 9098
    protocol    = "tcp"
    cidr_blocks = [var.vpc_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_msk_cluster" "main" {
  cluster_name           = "${var.project}-${var.environment}"
  kafka_version          = var.kafka_version
  number_of_broker_nodes = var.broker_count

  broker_node_group_info {
    instance_type   = var.instance_type
    client_subnets  = var.subnet_ids
    security_groups = [aws_security_group.msk.id]
    storage_info {
      ebs_storage_info { volume_size = 20 }
    }
  }

  encryption_info {
    encryption_in_transit {
      client_broker = "TLS"
      in_cluster    = true
    }
  }

  client_authentication {
    sasl {
      scram = true
    }
  }
}

resource "aws_msk_scram_secret_association" "main" {
  count           = length(var.scram_secret_arns) > 0 ? 1 : 0
  cluster_arn     = aws_msk_cluster.main.arn
  secret_arn_list = var.scram_secret_arns
}

output "bootstrap_brokers" { value = aws_msk_cluster.main.bootstrap_brokers }
output "bootstrap_brokers_sasl_scram" { value = aws_msk_cluster.main.bootstrap_brokers_sasl_scram }
