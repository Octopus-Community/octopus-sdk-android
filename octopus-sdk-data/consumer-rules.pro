# Rules for gRPC
-keepclassmembers class io.grpc.okhttp.OkHttpChannelBuilder {
    io.grpc.okhttp.OkHttpChannelBuilder forTarget(java.lang.String);
    io.grpc.okhttp.OkHttpChannelBuilder scheduledExecutorService(java.util.concurrent.ScheduledExecutorService);
    io.grpc.okhttp.OkHttpChannelBuilder sslSocketFactory(javax.net.ssl.SSLSocketFactory);
    io.grpc.okhttp.OkHttpChannelBuilder transportExecutor(java.util.concurrent.Executor);
}

# Keep gRPC generated classes
-keep class * extends io.grpc.stub.AbstractStub { *; }
-keep class * extends io.grpc.stub.StubFactory { *; }
-keep class * extends io.grpc.stub.AbstractAsyncStub { *; }
-keep class * extends io.grpc.stub.AbstractBlockingStub { *; }
-keep class * extends io.grpc.stub.AbstractFutureStub { *; }

# For Protocol Buffers
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# For OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# For gRPC-specific classes
-dontwarn io.grpc.**
# The `class` keyword already matches interfaces, so a single rule covers them.
-keep class io.grpc.** { *; }

# For Kotlin Coroutines with gRPC
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# For Guava, which gRPC needs at runtime
#
# gRPC's own classes are kept above, but the Guava they call arrives only as a transitive
# dependency of `grpc-core`, and no consumer rule protected it. A host app that shrinks its build
# (`minifyEnabled true`) therefore ran its own R8 over Guava, and the SDK died on that host's very
# first `OctopusSDK.initialize()` with `NoSuchMethodError: No static method
# isNullOrEmpty(Ljava/lang/String;)Z in class Lcom/google/common/base/Strings;` inside
# `io.grpc.internal.GrpcUtil`, followed by `NoClassDefFoundError: Failed resolution of:
# Lio/grpc/LoadBalancerRegistry; (erroneous class)` once that static initializer had failed.
#
# Note the `-keep class io.grpc.** { *; }` above makes every gRPC member an R8 root, so tracing
# normally reaches the Guava that gRPC calls and retains it: a missing `Strings.isNullOrEmpty`,
# which has existed for the whole life of Guava, is just as consistent with a second or older
# Guava on the host's classpath as with over-shrinking. Rather than turn on one root-cause story,
# these rules make Guava's fate independent of the host's shrinker and of tracing order.
#
# The whole package is kept on purpose rather than the handful of classes named in one stack
# trace: measured against the gRPC jars on the runtime classpath, gRPC references seven Guava
# packages (`base`, `util.concurrent`, `collect`, `io`, `net`, `math`, `primitives`), so a
# per-class list would keep passing until the next gRPC bump reached one more member — and the
# call site that breaks is `initialize()`, the worst place to regress. This does keep Guava out of
# the host's shrinking pass, a size cost in every minified consumer build (+22% on this repo's
# sample APK when measured with the sample temporarily minified), which is the deliberate trade
# for an SDK that cannot start otherwise.
#
# The `-dontwarn` matches the `-dontwarn io.grpc.**` above and is not free: a host that shades or
# excludes Guava altogether would otherwise get a hard "Missing class" error at build time, and
# now builds clean instead. That is the accepted cost of not emitting warnings for a package the
# host is not expected to reason about.
-keep class com.google.common.** { *; }
-dontwarn com.google.common.**
