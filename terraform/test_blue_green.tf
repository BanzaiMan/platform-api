module "test_blue_green_cluster_asg" {
  source                = "./ecs_asg_play"
  asg_name              = "test-blue-green-cluster"
  subnet_list           = ["${module.vpc_test_blue_green.subnets}"]
  key_name              = "${var.key_name}"
  instance_profile_name = "${module.ecs_test_blue_green_iam.instance_profile_name}"
  user_data             = "${module.test_blue_green_userdata.rendered}"
  vpc_id                = "${module.vpc_test_blue_green.vpc_id}"
  instance_type         = "t2.medium"
  admin_cidr_ingress    = "${var.admin_cidr_ingress}"
}

module "vpc_test_blue_green" {
  source     = "./network"
  cidr_block = "10.60.0.0/16"
  az_count   = "2"
}

module "ecs_test_blue_green_iam" {
  source = "./ecs_iam"
  name   = "test_blue_green"
}

resource "aws_ecs_cluster" "test_blue_green" {
  name = "test_blue_green_cluster"
}

module "test_blue_green_userdata" {
  source            = "./userdata"
  cluster_name      = "${aws_ecs_cluster.test_blue_green.name}"
}

module "hello_world" {
  source           = "./services"
  name             = "hello_world"
  cluster_id       = "${aws_ecs_cluster.test_blue_green.id}"
  task_role_arn    = "${module.ecs_hello_world_iam.task_role_arn}"
  vpc_id           = "${module.vpc_test_blue_green.vpc_id}"
  listener_arn     = "${module.test_blue_green_alb.listener_arn}"
  healthcheck_path = "/"
  container_name   = "hello_world"
  container_port   = "80"

  container_path = "/tmp"

  template_name  = "single_image_with_volume"
  docker_image   = "tutum/hello-world"

  environment_vars = <<EOF
  [
  ]
  EOF

  config_key        = ""
  infra_bucket      = ""
  is_config_managed = false
}

module "ecs_hello_world_iam" {
  source = "./ecs_iam"
  name   = "hello_world"
}

module "test_blue_green_alb" {
  source                       = "./ecs_alb"
  name                         = "tbg"
  subnets                      = ["${module.vpc_test_blue_green.subnets}"]
  loadbalancer_security_groups = ["${module.test_blue_green_cluster_asg.loadbalancer_sg_id}"]
  certificate_arn              = "arn:aws:acm:eu-west-1:760097843905:certificate/23957e05-0ecf-43dd-b9e1-eee44468ddc5"
  vpc_id                       = "${module.vpc_test_blue_green.vpc_id}"
}