package com.octopuscommunity.sdk

/**
 * Marks declarations that are **internal** to the Octopus SDK and its sample app.
 *
 * APIs marked with this annotation are not part of the public, supported surface of the SDK.
 * They may change, be removed, or behave unexpectedly without notice between any versions,
 * including patch releases.
 *
 * Integrators should **not** use these APIs in production code. They exist solely to support
 * the Octopus internal sample app, the Octopus wrapper SDKs (Flutter, React Native, Unity),
 * automated tests, and development workflows.
 *
 * To use an API marked with this annotation, opt in explicitly:
 * ```kotlin
 * @OptIn(InternalOctopusApi::class)
 * fun myFunction() { ... }
 * ```
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "This is an internal Octopus SDK API intended for the Octopus wrapper SDKs, " +
            "development and testing only. " +
            "It is not part of the supported public API and may change or be removed at any time.",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.TYPEALIAS,
)
annotation class InternalOctopusApi
