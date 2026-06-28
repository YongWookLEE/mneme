package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.search.SearchFilter
import com.mneme.search.SearchHit
import com.mneme.search.SearchService
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * 하이브리드 검색 REST. `GET /api/search?q=…&folderExtId=…&tags=a,b&from=&to=&limit=`.
 */
@RestController
@RequestMapping("/api/search")
class SearchController(
    private val searchService: SearchService,
    private val userResolver: AuthenticatedUserResolver,
) {
    @GetMapping
    fun search(
        @RequestParam q: String,
        @RequestParam(required = false) folderExtId: String?,
        @RequestParam(required = false) tags: String?,
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false, defaultValue = "20") limit: Int,
    ): List<SearchResponse> {
        if (q.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        val userId = userResolver.currentUserId()
        val filter =
            SearchFilter(
                folderId = folderExtId?.let { PrefixedId.parse(it, PrefixedId.Prefix.FOLDER).uuid },
                tagNames =
                    tags
                        ?.split(",")
                        ?.map { it.trim().lowercase() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList(),
                from = from?.let { parseTs(it) },
                to = to?.let { parseTs(it) },
            )
        return searchService.search(userId, q, filter, limit).map { it.toResponse() }
    }

    private fun parseTs(value: String): OffsetDateTime =
        try {
            OffsetDateTime.parse(value)
        } catch (e: DateTimeParseException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST)
        }

    private fun SearchHit.toResponse(): SearchResponse =
        SearchResponse(
            extId = PrefixedId(PrefixedId.Prefix.MEMORY, id).format(),
            folderExtId = PrefixedId(PrefixedId.Prefix.FOLDER, folderId).format(),
            title = title,
            summary = summary,
            score = score,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    data class SearchResponse(
        val extId: String,
        val folderExtId: String,
        val title: String,
        val summary: String?,
        val score: Double,
        val createdAt: OffsetDateTime,
        val updatedAt: OffsetDateTime,
    )
}
