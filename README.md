# Virgin Australia booking tests: Java, TestNG, RestAssured, Bazel

Two live API checks against the Virgin Australia booking site (`book.virginaustralia.com`), both taken from DevTools while searching Sydney to Brisbane.

## What is in here

| Path | What it is |
| --- | --- |
| `api-tests/` | RestAssured + TestNG. One target, two tests |
| `tools/testng/` | A Bazel macro that runs TestNG (Bazel's built-in runner only knows JUnit) |

`//api-tests:virgin_booking_test` calls the booking site:

- `GET .../passengerConfiguration.json`, the passenger-form config. It checks adult age bounds, name-field order, and the unaccompanied-minor rule. The URL contains the booking-app build id, so when Virgin ships a new build the test starts returning 404 and the constant in `PassengerConfigurationTest` has to be updated.
- `POST /api/graphql` operation `bookingAirSearch`. The response field `originalResponse` is a large nested shopping document. The test walks one path through it with RestAssured JsonPath (first offer, SYD to BNE, airline `VA`, a price in AUD) instead of comparing the whole body. Header `x-sabre-storefront: VADX` is required. Brisbane's airport code is `BNE`. The departure date is fourteen days ahead. Imperva blocks that POST when RestAssured's Apache client sends it (HTTP 403) and allows the JDK `HttpClient`, so the call uses the JDK client and JsonPath does the checks.

## Run

You need [Bazelisk](https://github.com/bazelbuild/bazelisk) on your `PATH` as `bazel`. It reads `.bazelversion` and downloads Bazel 9.3.2. Bazel then downloads its own JDK 21, so a system JDK is optional.

```bash
bazel test //...
bazel test //api-tests:virgin_booking_test
bazel build //...
bazel test --config=hermetic //...   # skips the live calls; nothing else is left to run
```

The target is tagged `external`, so Bazel does not cache a passing result. The first run downloads Maven artifacts and the JDK. TestNG's HTML report lands in `bazel-testlogs/api-tests/virgin_booking_test/test.outputs/testng/`. Bazel also writes JUnit XML to `test.xml` next to `test.log`.

## IntelliJ IDEA

Use the JetBrains Bazel plugin (generally available since July 2025, including on Windows). The older "Bazel for IntelliJ" plugin by Google is deprecated. Tests here are Bazel targets with a TestNG main class, so run them from the Bazel tool window, not from the ordinary JUnit gutter on a method.

1. Install Git and Bazelisk.
   - Windows, PowerShell: `winget install Bazel.Bazelisk` (this puts both `bazel` and `bazelisk` on `PATH`). Alternatives: `choco install bazelisk` or `scoop install bazelisk`.
   - macOS: `brew install bazelisk`.
   - Close and reopen the terminal, then `bazel version` should print 9.3.2 inside this repo.
2. Windows only, once per machine, so Bazel can cope with long paths:
   ```powershell
   git config --global core.longpaths true
   ```
   If a build later complains about a path that is still too long, create `user.bazelrc` in the repo (it is gitignored) with:
   ```
   startup --output_user_root=C:/_bzl
   ```
3. Install [IntelliJ IDEA](https://www.jetbrains.com/idea/) (Community is enough). `Ctrl+Alt+S` → Plugins → Marketplace → search **Bazel** → Install the one published by JetBrains → restart.
4. File → Open and choose this repository folder (the one that contains `MODULE.bazel`). Trust the project. The plugin syncs with Bazel; the first sync downloads the JDK and dependencies and takes a few minutes.
5. View → Tool Windows → Bazel. Right-click `//api-tests:virgin_booking_test` and choose **Test**.
6. To run the whole repo the way CI would: View → Tool Windows → Terminal, then `bazel test //...`.

A green Bazel sync is the project model. There is no Maven or Gradle file to import.

## Windows notes

- Bazelisk via `winget install Bazel.Bazelisk`, plus `git config --global core.longpaths true`.
- JDK 21 is downloaded by Bazel (`.bazelrc` sets `remotejdk_21`). A local JDK is not used for the build.
- `.bazelrc` enables runfiles on Windows (`build:windows --enable_runfiles`). The TestNG suite is loaded from the classpath, so the test does not depend on runfile path separators.
- Personal flags go in `user.bazelrc`, which is gitignored. Use forward slashes in paths there.
