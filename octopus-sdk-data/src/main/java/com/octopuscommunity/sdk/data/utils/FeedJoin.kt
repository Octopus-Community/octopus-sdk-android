package com.octopuscommunity.sdk.data.utils

import com.octopuscommunity.sdk.domain.model.Feed

/**
 * Joins a feed page with the locally cached entities it points at.
 *
 * The naive form scans the entity list once per feed item
 * (`entities.firstOrNull { it.id == feedItem.itemId }`), so the join costs
 * `O(feedItems × entities)`. That cost is paid again on every Room emission, over the whole
 * accumulated feed rather than over the page that changed, which is what makes it visible while
 * scrolling. Indexing the entities by id once brings the join down to `O(feedItems + entities)`.
 *
 * Duplicate ids keep the first entity, exactly like the `firstOrNull` scan it replaces: the chunked
 * `WHERE id IN (...)` queries can return the same row twice when one id lands in two chunks.
 */
internal fun <EntityT : Any, ResultT : Any> List<Feed.Item>.joinLocalEntities(
    entities: List<EntityT>,
    entityId: (EntityT) -> String,
    transform: (feedItem: Feed.Item, entity: EntityT) -> ResultT?,
): List<ResultT> {
    val entitiesById = entities.indexById(entityId)
    return mapNotNull { feedItem ->
        entitiesById[feedItem.itemId]?.let { entity -> transform(feedItem, entity) }
    }
}

/**
 * [joinLocalEntities] for a feed whose items also carry a featured child
 * ([Feed.Item.featuredChildId]), joined from a second entity list.
 */
internal fun <EntityT : Any, ChildT : Any, ResultT : Any> List<Feed.Item>.joinLocalEntities(
    entities: List<EntityT>,
    entityId: (EntityT) -> String,
    children: List<ChildT>,
    childId: (ChildT) -> String,
    transform: (feedItem: Feed.Item, entity: EntityT, child: ChildT?) -> ResultT?,
): List<ResultT> {
    val entitiesById = entities.indexById(entityId)
    val childrenById = children.indexById(childId)
    return mapNotNull { feedItem ->
        entitiesById[feedItem.itemId]?.let { entity ->
            transform(feedItem, entity, feedItem.featuredChildId?.let { childrenById[it] })
        }
    }
}

/**
 * Indexes [this] by [id], first occurrence wins.
 *
 * Not `associateBy`, which keeps the last occurrence: the scan this index replaces kept the first.
 */
private fun <T : Any> List<T>.indexById(id: (T) -> String): Map<String, T> {
    val index = HashMap<String, T>(size)
    forEach { element ->
        val key = id(element)
        if (!index.containsKey(key)) {
            index[key] = element
        }
    }
    return index
}
