data "aws_availability_zones" "available" {
  state = "available"
}

locals {
  subnet_az_count = max(length(var.private_subnet_cidrs), length(var.public_subnet_cidrs))
  selected_azs    = length(var.availability_zones) > 0 ? var.availability_zones : slice(data.aws_availability_zones.available.names, 0, local.subnet_az_count)

  private_subnet_map = {
    for index, cidr_block in var.private_subnet_cidrs :
    tostring(index) => {
      cidr_block = cidr_block
      az         = local.selected_azs[index]
    }
  }

  public_subnet_map = {
    for index, cidr_block in var.public_subnet_cidrs :
    tostring(index) => {
      cidr_block = cidr_block
      az         = local.selected_azs[index]
    }
  }
}

resource "aws_vpc" "dev" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = {
    Name = "${local.name_prefix}-vpc"
  }
}

resource "aws_subnet" "private" {
  for_each = local.private_subnet_map

  vpc_id            = aws_vpc.dev.id
  cidr_block        = each.value.cidr_block
  availability_zone = each.value.az

  tags = {
    Name = "${local.name_prefix}-private-${each.value.az}"
    Tier = "private"
  }
}

resource "aws_subnet" "public" {
  for_each = local.public_subnet_map

  vpc_id                  = aws_vpc.dev.id
  cidr_block              = each.value.cidr_block
  availability_zone       = each.value.az
  map_public_ip_on_launch = true

  tags = {
    Name = "${local.name_prefix}-public-${each.value.az}"
    Tier = "public"
  }
}

resource "aws_internet_gateway" "dev" {
  vpc_id = aws_vpc.dev.id

  tags = {
    Name = "${local.name_prefix}-igw"
  }
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.dev.id

  tags = {
    Name = "${local.name_prefix}-private-rt"
  }
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.dev.id

  tags = {
    Name = "${local.name_prefix}-public-rt"
  }
}

resource "aws_route" "public_default" {
  route_table_id         = aws_route_table.public.id
  destination_cidr_block = "0.0.0.0/0"
  gateway_id             = aws_internet_gateway.dev.id
}

resource "aws_route_table_association" "private" {
  for_each = aws_subnet.private

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private.id
}

resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}
