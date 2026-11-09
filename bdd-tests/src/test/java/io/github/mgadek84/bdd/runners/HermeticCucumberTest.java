package io.github.mgadek84.bdd.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/** Scenarios that need no internet: gRPC against the embedded ChessService. */
@CucumberOptions(
    features = "classpath:features",
    glue = "io.github.mgadek84.bdd.steps",
    tags = "not @lichess")
public class HermeticCucumberTest extends AbstractTestNGCucumberTests {

  static {
    CucumberReports.configure("cucumber-hermetic");
  }
}
