package com.wire.kalium.persistence.paging

/**
 * Original source https://github.com/cashapp/sqldelight/tree/master/extensions/android-paging3/src/main/java/app/cash/sqldelight/paging3
 * Modified to fix instabilities and crashes on our setup.
 *
 * Remove and use SQLDelight's implementation once the following issues are fixed and a patch is released:
 * - https://github.com/cashapp/sqldelight/issues/2591
 * - https://github.com/cashapp/sqldelight/issues/2434
 */

/*
 * Copyright (C) 2016 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import androidx.paging.PagingSource
import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlin.properties.Delegates

internal abstract class QueryPagingSource<Key : Any, RowType : Any> :
    PagingSource<Key, RowType>(),
    Query.Listener {

    protected var currentQuery: Query<RowType>? by Delegates.observable(null) { _, old, new ->
        old?.removeListener(this)
        new?.addListener(this)
    }

    init {
        registerInvalidatedCallback {
            currentQuery?.removeListener(this)
            currentQuery = null
        }
    }

    final override fun queryResultsChanged() = invalidate()
}

/**
 * Create a [PagingSource] that pages through results according to queries generated by
 * [queryProvider]. Queries returned by [queryProvider] should expect to do SQL offset/limit
 * based paging. For that reason, [countQuery] is required to calculate pages and page offsets.
 *
 * An example query returned by [queryProvider] could look like:
 *
 * ```sql
 * SELECT value FROM numbers
 * LIMIT 10
 * OFFSET 100;
 * ```
 *
 * Queries will be executed on [dispatcher].
 */
@Suppress("FunctionName")
fun <RowType : Any> QueryPagingSource(
    countQuery: Query<Long>,
    transacter: Transacter,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
    queryProvider: (limit: Long, offset: Long) -> Query<RowType>,
): PagingSource<Long, RowType> = OffsetQueryPagingSource(
    queryProvider,
    countQuery,
    transacter,
    dispatcher
)
