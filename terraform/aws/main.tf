terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }

  backend "s3" {
    bucket = "notification-hub-tfstate"
    key    = "prod/terraform.tfstate"
    region = "ap-northeast-2"
  }
}

provider "aws" {
  region = var.aws_region
}

module "vpc" {
  source = "./modules/vpc"

  project     = var.project
  environment = var.environment
  cidr_block  = var.vpc_cidr
}

module "eks" {
  source = "./modules/eks"

  project        = var.project
  environment    = var.environment
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  node_min_size  = 2
  node_max_size  = 10
  node_instance_type = "t3.medium"
}

module "rds" {
  source = "./modules/rds"

  project        = var.project
  environment    = var.environment
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  instance_class = "db.t3.medium"
  db_name        = "notification_hub"
  username       = var.db_username
  password       = var.db_password
}

module "msk" {
  source = "./modules/msk"

  project        = var.project
  environment    = var.environment
  vpc_id         = module.vpc.vpc_id
  subnet_ids     = module.vpc.private_subnet_ids
  instance_type  = "kafka.t3.small"
  kafka_version  = "3.5.1"
  broker_count   = 2
}

module "elasticache" {
  source = "./modules/elasticache"

  project      = var.project
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
  subnet_ids   = module.vpc.private_subnet_ids
  node_type    = "cache.t3.micro"
}

module "documentdb" {
  source = "./modules/documentdb"

  project      = var.project
  environment  = var.environment
  vpc_id       = module.vpc.vpc_id
  subnet_ids   = module.vpc.private_subnet_ids
  instance_class = "db.t3.medium"
}
