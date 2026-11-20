package com.octopuscommunity.sdk.domain.model

/**
 * Top-level alias for [Post.CustomAction], shared by posts and groups.
 *
 * Use [Post.CustomAction] for post call-to-action values. iOS has no public equivalent
 * of this alias; it remains available until its removal in 2.0.
 */
@Deprecated(message = "Not part of the iOS API; removed in 2.0.", level = DeprecationLevel.WARNING)
typealias CustomAction = Post.CustomAction
