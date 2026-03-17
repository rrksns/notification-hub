variable "project" {}
variable "environment" {}
variable "vpc_id" {}
variable "subnet_ids" { type = list(string) }
variable "instance_class" { default = "db.t3.medium" }
variable "db_name" {}
variable "username" { sensitive = true }
variable "password" { sensitive = true }

resource "aws_db_subnet_group" "main" {
  name       = "${var.project}-db-subnet"
  subnet_ids = var.subnet_ids
}

resource "aws_db_instance" "main" {
  identifier        = "${var.project}-${var.environment}-mysql"
  engine            = "mysql"
  engine_version    = "8.0"
  instance_class    = var.instance_class
  allocated_storage = 20
  db_name           = var.db_name
  username          = var.username
  password          = var.password

  db_subnet_group_name   = aws_db_subnet_group.main.name
  multi_az               = true
  skip_final_snapshot    = false
  deletion_protection    = true

  tags = { Name = "${var.project}-rds" }
}

output "endpoint" { value = aws_db_instance.main.endpoint }
