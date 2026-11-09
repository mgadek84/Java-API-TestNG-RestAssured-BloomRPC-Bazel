package io.github.mgadek84.bdd.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Scenarios that call lichess.org. Runs serially: AbstractTestNGCucumberTests' data provider is
 * not parallel unless overridden, and Lichess allows one request at a time.
 */
@CucumberOptions(
    features = "classpath:features",
    glue = "io.github.mgadek84.bdd.steps",
    tags = "@lichess")
public class LichessCucumberTest extends AbstractTestNGCucumberTests {

  static {
    CucumberReports.configure("cucumber-lichess");
  }
}
