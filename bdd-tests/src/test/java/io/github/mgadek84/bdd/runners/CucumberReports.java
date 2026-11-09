package io.github.mgadek84.bdd.runners;

import java.nio.file.Path;

/**
 * Points Cucumber's HTML/JSON reports at Bazel's undeclared outputs directory, so they end up in
 * {@code bazel-testlogs/<target>/test.outputs/}.
 */
final class CucumberReports {

  private CucumberReports() {}

  static void configure(String reportName) {
    if (System.getProperty("cucumber.plugin") != null) {
      return;
    }
    String outputs = System.getenv("TEST_UNDECLARED_OUTPUTS_DIR");
    String dir = outputs != null ? outputs : Path.of("target", "cucumber").toString();
    String base = Path.of(dir, reportName).toString().replace('\\', '/');
    System.setProperty(
        "cucumber.plugin", "pretty, summary, html:" + base + ".html, json:" + base + ".json");
  }
}
