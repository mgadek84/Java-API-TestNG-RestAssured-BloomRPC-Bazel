"""java_grpc_library built from prebuilt binaries only.

grpc-java ships its own java_grpc_library rule, but it compiles protoc and the
gRPC code generator from C++ sources. That needs a C++ toolchain (MSVC Build
Tools on Windows) and adds minutes to a cold build. This rule produces the same
*Grpc.java stubs using:
  * the prebuilt protoc that protobuf registers as its proto toolchain, and
  * the prebuilt protoc-gen-grpc-java published by grpc-java on Maven Central
    (downloaded per OS in MODULE.bazel).
"""

load("@com_google_protobuf//bazel/common:proto_common.bzl", "proto_common")
load("@com_google_protobuf//bazel/common:proto_info.bzl", "ProtoInfo")
load("@com_google_protobuf//bazel/common:proto_lang_toolchain_info.bzl", "ProtoLangToolchainInfo")
load("@rules_java//java:defs.bzl", "java_library")

def _grpc_java_srcjar_impl(ctx):
    srcjar = ctx.actions.declare_file(ctx.label.name + ".srcjar")
    proto_common.compile(
        actions = ctx.actions,
        proto_info = ctx.attr.proto[ProtoInfo],
        proto_lang_toolchain_info = ctx.attr._toolchain[ProtoLangToolchainInfo],
        generated_files = [srcjar],
        experimental_output_files = "single",
    )
    return [DefaultInfo(files = depset([srcjar]))]

_grpc_java_srcjar = rule(
    implementation = _grpc_java_srcjar_impl,
    attrs = {
        "proto": attr.label(mandatory = True, providers = [ProtoInfo]),
        "_toolchain": attr.label(
            default = Label("//tools/grpc:grpc_java_lang_toolchain"),
            providers = [ProtoLangToolchainInfo],
        ),
    },
)

def java_grpc_library(name, srcs, deps, **kwargs):
    """Generates and compiles gRPC service stubs for one proto_library.

    Same signature as grpc-java's rule, so switching back is a one-line load change.

    Args:
      name: target name.
      srcs: a single proto_library containing the service.
      deps: the java_proto_library for the same proto (message classes).
      **kwargs: passed to java_library (visibility, tags...).
    """
    if len(srcs) != 1:
        fail("java_grpc_library: srcs must contain exactly one proto_library")
    _grpc_java_srcjar(
        name = name + "_srcjar",
        proto = srcs[0],
        visibility = ["//visibility:private"],
    )
    java_library(
        name = name,
        srcs = [":" + name + "_srcjar"],
        deps = deps + [Label("//tools/grpc:grpc_java_runtime")],
        exports = [Label("//tools/grpc:grpc_java_runtime")],
        **kwargs
    )
