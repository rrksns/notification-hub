terraform {
  required_providers {
    docker = {
      source  = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {}

resource "docker_network" "notification_hub" {
  name = "notification-hub-network"
}

resource "docker_image" "mysql" {
  name = "mysql:8.0"
}

resource "docker_container" "mysql" {
  name  = "notification-hub-mysql"
  image = docker_image.mysql.image_id

  env = [
    "MYSQL_ROOT_PASSWORD=root1234",
    "MYSQL_DATABASE=notification_hub",
    "MYSQL_USER=nhub",
    "MYSQL_PASSWORD=nhub1234"
  ]

  ports {
    internal = 3306
    external = 3307
  }

  networks_advanced {
    name = docker_network.notification_hub.name
  }
}

resource "docker_image" "redis" {
  name = "redis:7.2"
}

resource "docker_container" "redis" {
  name  = "notification-hub-redis"
  image = docker_image.redis.image_id

  ports {
    internal = 6379
    external = 6379
  }

  networks_advanced {
    name = docker_network.notification_hub.name
  }
}

resource "docker_image" "mongodb" {
  name = "mongo:7.0"
}

resource "docker_container" "mongodb" {
  name  = "notification-hub-mongodb"
  image = docker_image.mongodb.image_id

  env = [
    "MONGO_INITDB_ROOT_USERNAME=nhub",
    "MONGO_INITDB_ROOT_PASSWORD=nhub1234",
    "MONGO_INITDB_DATABASE=analytics"
  ]

  ports {
    internal = 27017
    external = 27017
  }

  networks_advanced {
    name = docker_network.notification_hub.name
  }
}

resource "docker_image" "kafka" {
  name = "apache/kafka:3.7.0"
}

resource "docker_container" "kafka" {
  name  = "notification-hub-kafka"
  image = docker_image.kafka.image_id

  env = [
    "KAFKA_NODE_ID=1",
    "KAFKA_PROCESS_ROLES=broker,controller",
    "KAFKA_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093",
    "KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092",
    "KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER",
    "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT",
    "KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093",
    "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1",
    "CLUSTER_ID=MkU3OEVBNTcwNTJENDM2Qk"
  ]

  ports {
    internal = 9092
    external = 9092
  }

  networks_advanced {
    name = docker_network.notification_hub.name
  }
}
