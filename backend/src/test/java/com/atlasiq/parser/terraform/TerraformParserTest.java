package com.atlasiq.parser.terraform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;
class TerraformParserTest {
 @TempDir Path root;
 @Test void parsesResourcesModulesAndDependencies() throws Exception {
  Files.writeString(root.resolve("main.tf"), """
provider "aws" {}
resource "aws_vpc" "main" { cidr_block = "10.0.0.0/16" }
resource "aws_subnet" "app" { vpc_id = aws_vpc.main.id }
module "service" { source = "./service" }
output "vpc_id" { value = aws_vpc.main.id }
""");
  var result=new TerraformParser().parse(root);
  assertTrue(result.nodes().stream().anyMatch(n->n.id().equals("terraform:aws_vpc.main")));
  assertTrue(result.nodes().stream().anyMatch(n->n.id().equals("terraform:module.service")));
  assertTrue(result.edges().stream().anyMatch(e->e.from().equals("terraform:aws_subnet.app")&&e.to().equals("terraform:aws_vpc.main")));
 }
}
