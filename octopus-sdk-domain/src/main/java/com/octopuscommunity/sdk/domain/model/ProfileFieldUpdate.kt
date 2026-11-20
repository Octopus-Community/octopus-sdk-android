package com.octopuscommunity.sdk.domain.model

/** A profile field's update information, with a context of type [T] when updated. */
sealed interface ProfileFieldUpdate<out T> {
    /** Whether this field has been updated. */
    val isUpdated: Boolean get() = this is Updated<*>

    /** This field has not changed. */
    data object Unchanged : ProfileFieldUpdate<Nothing>

    /**
     * This field has changed.
     * @property value Context describing the updated field.
     */
    data class Updated<T>(
        val value: T,
    ) : ProfileFieldUpdate<T>
}
