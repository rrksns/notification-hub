variable "project" {}
variable "environment" {}
variable "vpc_id" {}
variable "subnet_ids" { type = list(string) }
variable "instance_class" { default = "db.t3.medium" }

resource "aws_docdb_subnet_group" "main" {
  name       = "${var.project}-docdb-subnet"
  subnet_ids = var.subnet_ids
}

resource "aws_docdb_cluster" "main" {
  cluster_identifier = "${var.project}-${var.environment}-docdb"
  engine             = "docdb"
  master_username    = "nhub"
  master_password    = "nhub1234changeme"
  db_subnet_group_name = aws_docdb_subnet_group.main.name
  skip_final_snapshot  = false
}

resource "aws_docdb_cluster_instance" "main" {
  count              = 2
  identifier         = "${var.project}-docdb-${count.index}"
  cluster_identifier = aws_docdb_cluster.main.id
  instance_class     = var.instance_class
}

output "endpoint" { value = aws_docdb_cluster.main.endpoint }
