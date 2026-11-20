package com.octopuscommunity.sdk

/**
 * Server endpoint the SDK targets at initialization time.
 *
 * Pass an [ApiServer] to [OctopusSDK.initialize] to route the SDK's gRPC traffic
 * to a custom [host] and [port]. When no [ApiServer] is provided, the SDK uses
 * the Octopus default endpoint over TLS.
 *
 * The host is validated at construction time and the constructor throws a
 * [ValidationError] on invalid input. Accepted forms:
 * - a DNS name (e.g. `"api.example.com"`)
 * - an IPv4 literal (e.g. `"192.0.2.10"`)
 * - an IPv6 literal, either bracketed (`"[::1]"`) or unbracketed (`"::1"`)
 *
 * The host must not contain a scheme (`https://`), an embedded port (`:443`),
 * a path, or whitespace. The constructor throws a typed [ValidationError]
 * (subclass of [IllegalArgumentException]) so callers may either catch the
 * generic exception or `when` over the sealed hierarchy.
 *
 * ## Transport security
 *
 * The connection is established over TLS. The public [ApiServer] does not
 * expose a cleartext toggle; pointing the SDK at a server that does not
 * accept TLS will surface as a gRPC connection failure at the first call.
 *
 * @property host Server host. Hostname-only — see the accepted / rejected
 * forms above.
 * @property port Server port. Defaults to `443`.
 */
data class ApiServer(
    val host: String,
    val port: Int = 443,
) {
    init {
        validate(host)
    }

    /**
     * The [host] with surrounding IPv6 brackets removed (if any), suitable
     * for handing off to gRPC channel builders that accept the address
     * literal without brackets.
     */
    internal val unbracketedHost: String get() = stripIPv6Brackets(host)

    /**
     * Failures thrown by the [ApiServer] constructor when [host] is
     * invalid. Subclasses [IllegalArgumentException] so callers that do
     * not need to discriminate by case can `catch (e: IllegalArgumentException)`;
     * the sealed hierarchy lets a caller `when` over the specific reason.
     */
    sealed class ValidationError(
        message: String,
    ) : IllegalArgumentException(message) {
        /** The host string is empty. */
        object EmptyHost : ValidationError(
            "ApiServer.ValidationError.EmptyHost: host must not be empty.",
        )

        /** The host string contains a scheme (e.g. `https://`). */
        object HostContainsScheme : ValidationError(
            "ApiServer.ValidationError.HostContainsScheme: " +
                "host must not contain a scheme (e.g. 'https://').",
        )

        /**
         * The host string contains a path or URL-authority metacharacter
         * (`/`, `?`, `#`, `@`), or a single `:` that looks like a port
         * separator. Use the [port] parameter instead.
         */
        object HostContainsPortOrPath : ValidationError(
            "ApiServer.ValidationError.HostContainsPortOrPath: " +
                "host must not contain a path, a port, or URL-authority metacharacters " +
                "('/', '?', '#', '@', ':'). Use the port parameter instead.",
        )

        /** The host string contains whitespace. */
        object HostContainsWhitespace : ValidationError(
            "ApiServer.ValidationError.HostContainsWhitespace: " +
                "host must not contain whitespace.",
        )

        /**
         * The host string is malformed as an IPv6 bracketed literal
         * (e.g. opening `[` without closing `]`, characters before or
         * after the brackets, or nested brackets).
         */
        object InvalidIPv6Bracketing : ValidationError(
            "ApiServer.ValidationError.InvalidIPv6Bracketing: " +
                "host is a malformed IPv6 bracketed literal.",
        )
    }

    internal companion object {
        @Throws(ValidationError::class)
        internal fun validate(host: String) {
            if (host.isEmpty()) throw ValidationError.EmptyHost
            if (host.any { it.isWhitespace() }) throw ValidationError.HostContainsWhitespace
            if (host.contains("://")) throw ValidationError.HostContainsScheme

            val forbidden = setOf('/', '?', '#', '@')
            if (host.any { it in forbidden }) throw ValidationError.HostContainsPortOrPath

            // Bracket / IPv6 handling.
            if (host.first() == '[') {
                if (host.last() != ']' || host.length < 3) throw ValidationError.InvalidIPv6Bracketing
                val inner = host.substring(1, host.length - 1)
                if (inner.contains('[') || inner.contains(']')) {
                    throw ValidationError.InvalidIPv6Bracketing
                }
                // `:` is allowed inside brackets (IPv6 separator). No further checks here —
                // gRPC will surface a connection error if the literal is invalid.
                return
            }
            // Stray brackets without leading `[` are malformed.
            if (host.contains('[') || host.contains(']')) {
                throw ValidationError.InvalidIPv6Bracketing
            }

            // Colon rule: exactly one `:` = looks like host:port, reject. Two or more
            // = unbracketed IPv6 literal, accept.
            val colonCount = host.count { it == ':' }
            if (colonCount == 1) throw ValidationError.HostContainsPortOrPath
        }

        private fun stripIPv6Brackets(host: String): String {
            if (host.length < 2 || host.first() != '[' || host.last() != ']') return host
            return host.substring(1, host.length - 1)
        }
    }
}
