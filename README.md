# Lichess API tests: Java, TestNG, RestAssured, Cucumber, gRPC, Bazel

A small, real API test framework against the public [Lichess API](https://lichess.org/api), plus a gRPC service of our own so BloomRPC and Java gRPC tests have something to call. Lichess itself only speaks HTTP/JSON.

Built as interview practice for an SDET role that asks for Java, TestNG, RestAssured, BDD (Cucumber), BloomRPC and Bazel in a monorepo.

## What is in here

| Path | What it is |
| --- | --- |
| `api-tests/` | RestAssured + TestNG against Lichess: specs, config, POJOs, custom AssertJ assertions, JSON Schema |
| `bdd-tests/` | Cucumber features on the TestNG runner. Steps reuse `api-tests` |
| `chess-service/` | `ChessService` gRPC server: FEN validation, and a gateway over `GET /api/user/{username}` |
| `proto/chess/v1/` | The `.proto` BloomRPC imports |
| `tools/testng/` | A Bazel macro that runs TestNG (Bazel's built-in runner only knows JUnit) |
| `tools/grpc/` | Generates Java gRPC stubs from prebuilt `protoc` binaries, so no C++ compiler is required |

Only unauthenticated Lichess endpoints are used: user profile, game export (JSON and PGN), puzzles, cloud eval. Tests run serially, wait at least 1 second between calls, and on HTTP 429 wait and retry once, then skip instead of failing.

## Run everything

You need [Bazelisk](https://github.com/bazelbuild/bazelisk) on your `PATH` as `bazel`. It reads `.bazelversion` and downloads Bazel 9.3.2. Bazel then downloads its own JDK 21, so a system JDK is optional.

```bash
bazel test //...          # everything, including the live Lichess tests
bazel build //...         # compile only
bazel test --config=hermetic //...   # no network: gRPC, FEN, rate-limit unit tests
bazel test --config=lichess //...    # only the live Lichess tests
```

Single targets:

```bash
bazel test //api-tests:lichess_api_test
bazel test //api-tests:framework_test
bazel test //chess-service:chess_service_test
bazel test //bdd-tests:bdd_hermetic_test
bazel test //bdd-tests:bdd_lichess_test
bazel run //chess-service:server          # gRPC on localhost:50051, for BloomRPC
```

The first run downloads Maven artifacts and the JDK. Later runs are incremental. Cucumber HTML reports land in `bazel-testlogs/bdd-tests/<target>/test.outputs/`.

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
5. View → Tool Windows → Bazel. You get the targets from the `BUILD.bazel` files.
6. Right-click a `*_test` target and choose **Test**. Double-click does the same. Useful ones:
   - `//chess-service:chess_service_test` and `//api-tests:framework_test` and `//bdd-tests:bdd_hermetic_test` need no internet.
   - `//api-tests:lichess_api_test` and `//bdd-tests:bdd_lichess_test` call lichess.org. Run them one at a time.
   - Right-click `//chess-service:server` and choose **Run** to start the gRPC server for BloomRPC.
7. To run the whole repo the way CI would: View → Tool Windows → Terminal, then `bazel test //...`.

A green Bazel sync is the project model. There is no Maven or Gradle file to import.

## BloomRPC

BloomRPC does not use server reflection, so you import the proto yourself.

1. Start the server: `bazel run //chess-service:server`. It prints `ChessService listening on localhost:50051 (plaintext)`.
2. In BloomRPC: the `+` next to the address → import `proto/chess/v1/chess_service.proto`. It has no imports, so no include path is needed.
3. Address: `localhost:50051`. Turn **TLS off**. BloomRPC calls this "TLS" or a lock icon; plaintext means no TLS.
4. `ValidateFen`, the starting position:

```json
{
  "fen": "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
}
```

Expect `valid: true`, `sideToMove: "WHITE"`, 16 pieces on each side.

5. `ValidateFen` with a broken position (status OK, `valid: false` and `errors`):

```json
{ "fen": "8/8/8/8/8/8/8/8 w - - 0 1" }
```

6. `ValidateFen` with `{ "fen": "" }` → gRPC status `INVALID_ARGUMENT`.
7. `GetPlayerProfile` (this one really calls Lichess):

```json
{ "username": "thibault" }
```

Expect `id: "thibault"` and a `ratings` list. An unknown name returns `NOT_FOUND`.

Postman and grpcurl can call the same address. They also need the proto file, because the server does not expose gRPC reflection (that would pull a C++ build of the reflection stubs into this repo).

## Windows notes

- Bazelisk via `winget install Bazel.Bazelisk`, plus `git config --global core.longpaths true`.
- No Visual Studio / C++ toolchain is required. Protobuf and the gRPC Java plugin are prebuilt binaries downloaded by Bazel (`MODULE.bazel`).
- JDK 21 is downloaded by Bazel (`.bazelrc` sets `remotejdk_21`). A local JDK is not used for the build.
- `.bazelrc` enables runfiles on Windows (`build:windows --enable_runfiles`). TestNG suites and feature files are loaded from the classpath, so tests do not depend on runfile path separators.
- Personal flags go in `user.bazelrc`, which is gitignored. Use forward slashes in paths there.

## Layout of a test

`LichessSpecs` builds the shared RestAssured request (base URL, `User-Agent`, `Accept: application/json`, timeouts). `UsersApi`, `GamesApi`, `PuzzlesApi` and `CloudEvalApi` are the API objects. Every call goes through `RateLimitedExecutor`. Tests assert either with RestAssured's Hamcrest DSL (`response.then().body(...)`) or by deserializing into a record and using `LichessAssertions`.

Cucumber step classes are thin: they call those same API objects and assertions. PicoContainer injects a fresh `ScenarioContext` per scenario.
