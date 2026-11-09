package io.github.mgadek84.testing.testng;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Collects TestNG results and writes them in the JUnit XML format Bazel expects at {@code
 * $XML_OUTPUT_FILE}, so CI systems and IDEs show individual test cases instead of one opaque
 * target.
 */
final class ResultCollector implements ITestListener, IConfigurationListener {

  private final List<ITestResult> results = new ArrayList<>();
  private int configurationFailures;

  @Override
  public synchronized void onTestSuccess(ITestResult result) {
    results.add(result);
  }

  @Override
  public synchronized void onTestFailure(ITestResult result) {
    results.add(result);
    System.err.println("FAILED: " + result.getTestClass().getName() + "." + displayName(result));
    System.err.println(stackTrace(result.getThrowable()));
  }

  @Override
  public synchronized void onTestSkipped(ITestResult result) {
    results.add(result);
    Throwable reason = result.getThrowable();
    System.err.println(
        "SKIPPED: "
            + result.getTestClass().getName()
            + "."
            + displayName(result)
            + (reason == null ? "" : " - " + reason.getMessage()));
  }

  @Override
  public synchronized void onTestFailedButWithinSuccessPercentage(ITestResult result) {
    results.add(result);
  }

  @Override
  public synchronized void onConfigurationFailure(ITestResult result) {
    configurationFailures++;
    System.err.println("Configuration method failed: " + displayName(result));
    if (result.getThrowable() != null) {
      result.getThrowable().printStackTrace();
    }
  }

  synchronized int total() {
    return results.size();
  }

  synchronized int configurationFailures() {
    return configurationFailures;
  }

  synchronized String summary() {
    long passed = count(ITestResult.SUCCESS);
    long failed = count(ITestResult.FAILURE) + count(ITestResult.SUCCESS_PERCENTAGE_FAILURE);
    long skipped = count(ITestResult.SKIP);
    return String.format(
        Locale.ROOT,
        "TestNG: %d tests, %d passed, %d failed, %d skipped, %d configuration failures",
        results.size(),
        passed,
        failed,
        skipped,
        configurationFailures);
  }

  synchronized void writeJUnitXml(Path file) throws IOException {
    long failures = count(ITestResult.FAILURE) + count(ITestResult.SUCCESS_PERCENTAGE_FAILURE);
    long skipped = count(ITestResult.SKIP);
    double totalSeconds = results.stream().mapToDouble(ResultCollector::seconds).sum();

    StringBuilder xml = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    xml.append(
        String.format(
            Locale.ROOT,
            "<testsuites><testsuite name=\"testng\" tests=\"%d\" failures=\"%d\" errors=\"0\""
                + " skipped=\"%d\" time=\"%.3f\">\n",
            results.size(),
            failures,
            skipped,
            totalSeconds));
    for (ITestResult result : results) {
      xml.append(
          String.format(
              Locale.ROOT,
              "  <testcase classname=\"%s\" name=\"%s\" time=\"%.3f\"",
              escape(result.getTestClass().getName()),
              escape(displayName(result)),
              seconds(result)));
      switch (result.getStatus()) {
        case ITestResult.FAILURE, ITestResult.SUCCESS_PERCENTAGE_FAILURE -> {
          Throwable t = result.getThrowable();
          xml.append(">\n    <failure message=\"")
              .append(escape(t == null ? "failed" : String.valueOf(t.getMessage())))
              .append("\">")
              .append(escape(stackTrace(t)))
              .append("</failure>\n  </testcase>\n");
        }
        case ITestResult.SKIP -> {
          Throwable t = result.getThrowable();
          xml.append(">\n    <skipped message=\"")
              .append(escape(t == null ? "skipped" : String.valueOf(t.getMessage())))
              .append("\"/>\n  </testcase>\n");
        }
        default -> xml.append("/>\n");
      }
    }
    xml.append("</testsuite></testsuites>\n");
    Files.createDirectories(file.toAbsolutePath().getParent());
    Files.writeString(file, xml, StandardCharsets.UTF_8);
  }

  private long count(int status) {
    return results.stream().filter(r -> r.getStatus() == status).count();
  }

  private static String displayName(ITestResult result) {
    String name = result.getMethod().getMethodName();
    Object[] params = result.getParameters();
    if (params == null || params.length == 0) {
      return name;
    }
    return name
        + Arrays.stream(params).map(String::valueOf).collect(Collectors.joining(", ", "(", ")"));
  }

  private static double seconds(ITestResult result) {
    return Math.max(0, result.getEndMillis() - result.getStartMillis()) / 1000.0;
  }

  private static String stackTrace(Throwable t) {
    if (t == null) {
      return "";
    }
    StringWriter writer = new StringWriter();
    t.printStackTrace(new PrintWriter(writer));
    return writer.toString();
  }

  private static String escape(String s) {
    StringBuilder out = new StringBuilder(s.length());
    for (char c : s.toCharArray()) {
      switch (c) {
        case '&' -> out.append("&amp;");
        case '<' -> out.append("&lt;");
        case '>' -> out.append("&gt;");
        case '"' -> out.append("&quot;");
        default -> {
          if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
            out.append(' ');
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }
}
