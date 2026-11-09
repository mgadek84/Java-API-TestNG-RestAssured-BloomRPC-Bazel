"""testng_test: run TestNG suites under `bazel test`.

Bazel's built-in java_test runner only understands JUnit. This macro swaps in a
small TestNG launcher (//tools/testng:runner) that runs the given suite XML
files, writes Bazel's test.xml (JUnit format) and maps results to an exit code:
failures fail the target, skips do not.
"""

load("@rules_java//java:defs.bzl", "java_test")

def testng_test(name, suites, runtime_deps = [], **kwargs):
    """Declares a java_test that runs TestNG suites.

    Args:
      name: target name.
      suites: classpath paths of testng.xml files (e.g. "suites/api.xml"),
        packaged via `resources`.
      runtime_deps: extra runtime dependencies.
      **kwargs: anything else java_test accepts (srcs, resources, tags, size...).
    """
    java_test(
        name = name,
        main_class = "io.github.mgadek84.testing.testng.BazelTestNgRunner",
        use_testrunner = False,
        args = suites,
        runtime_deps = runtime_deps + [
            "//tools/testng:runner",
            "@maven//:org_testng_testng",
        ],
        **kwargs
    )
