variable "task_name" {
  description = "Name of the task to create"
}

variable "template_name" {
  description = "Name of the template to use"
  default     = "default"
}

variable "nginx_uri" {
  description = "URI of container image for nginx"
}

variable "app_uri" {
  description = "URI of container image for app"
}

variable "aws_region" {
  description = "AWS Region the task will run in"
  default     = "eu-west-1"
}

variable "task_role_arn" {
  description = "ARN of IAM Role for task"
}

variable "volume_name" {
  description = "Name of volume to mount (if required)"
  default     = "ephemera"
}

variable "volume_host_path" {
  description = "Location of mount point on host path (if required)"
  default     = "/tmp"
}

variable "config_key" {
  description = "Location of config file within S3"
}

variable "infra_bucket" {
  description = "Location of infra bucket in S3"
}