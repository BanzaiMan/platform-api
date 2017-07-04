module "id_minter_topic" {
  source = "./sns"
  name   = "id_minter"
}

module "es_ingest_topic" {
  source = "./sns"
  name   = "es_ingest"
}

module "service_scheduler_topic" {
  source = "./sns"
  name   = "service_scheduler"
}

module "dynamo_capacity_topic" {
  source = "./sns"
  name   = "dynamo_capacity_requests"
}

module "ec2_terminating_topic" {
  source = "./sns"
  name   = "ec2_terminating_topic"
}

module "old_deployments" {
  source = "./sns"
  name   = "old_deployments"
}

# Alarm topics

module "dlq_alarm" {
  source = "./sns"
  name   = "dlq_alarm"
}

module "ec2_instance_terminating_for_too_long_alarm" {
  source = "./sns"
  name   = "ec2_instance_terminating_for_too_long_alarm"
}
