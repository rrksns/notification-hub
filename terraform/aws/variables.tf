variable "aws_region" {
  description = "AWS region"
  default     = "ap-northeast-2"
}

variable "project" {
  description = "Project name"
  default     = "notification-hub"
}

variable "environment" {
  description = "Environment (prod/staging/dev)"
  default     = "prod"
}

variable "vpc_cidr" {
  description = "VPC CIDR block"
  default     = "10.0.0.0/16"
}

variable "db_username" {
  description = "RDS master username"
  sensitive   = true
}

variable "db_password" {
  description = "RDS master password"
  sensitive   = true
}
