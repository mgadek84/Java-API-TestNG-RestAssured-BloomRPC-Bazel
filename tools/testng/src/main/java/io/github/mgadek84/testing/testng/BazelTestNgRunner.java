package io.github.mgadek84.testing.testng;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.testng.ITestNGListener;
import org.testng.TestNG;
import org.testng.xml.SuiteXmlParser;
import org.testng.xml.XmlSuite;

/**
 * Entry point used by the {@code testng_test} Bazel macro.
 *
 * <p>Arguments are classpath paths of TestNG suite files. Suites are loaded from the classpath
 * (not the file system) so the same target works on Linux, macOS and Windows without runfiles
 * path handling.
 *
 * <p>Exit code contract with Bazel: 0 when nothing failed (skipped tests are allowed), 1 on any
 * test or configuration failure, or when no test ran at all (which almost always means a broken
 * suite file).
 */
public final class BazelTestNgRunner {

  private BazelTestNgRunner() {}

  public static void main(String[] args) throws IOException {
    if (args.length == 0) {
      System.err.println("Usage: BazelTestNgRunner <suite.xml on classpath>...");
      System.exit(2);
    }

    List<XmlSuite> suites = new ArrayList<>();
    for (String resource : args) {
      suites.add(loadSuite(resource));
    }

    ResultCollector results = new ResultCollector();
    TestNG testng = new TestNG();
    testng.setXmlSuites(suites);
    testng.addListener((ITestNGListener) results);

    String outputsDir = System.getenv("TEST_UNDECLARED_OUTPUTS_DIR");
    if (outputsDir != null) {
      testng.setOutputDirectory(Path.of(outputsDir, "testng").toString());
    }

    testng.run();

    String xmlOutputFile = System.getenv("XML_OUTPUT_FILE");
    if (xmlOutputFile != null) {
      results.writeJUnitXml(Path.of(xmlOutputFile));
    }

    System.out.println(results.summary());
    boolean failed =
        testng.hasFailure()
            || testng.hasFailureWithinSuccessPercentage()
            || results.configurationFailures() > 0
            || results.total() == 0;
    System.exit(failed ? 1 : 0);
  }

  private static XmlSuite loadSuite(String resource) throws IOException {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try (InputStream in = loader.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalArgumentException("TestNG suite not found on classpath: " + resource);
      }
      return new SuiteXmlParser().parse(resource, in, true);
    }
  }
}
