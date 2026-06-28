package com.mneme.api

import com.mneme.id.PrefixedId
import com.mneme.memory.MemoryLinkRepository
import com.mneme.memory.MemoryRepository
import com.mneme.security.AuthenticatedUserResolver
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * `/api/graph` — 본인 메모리 노드와 wiki-link 엣지를 한 번에 내려준다.
 *
 * 노드: 활성 메모리. 엣지: forward link(소스 → 타겟). target_id=null인 깨진 링크는 별도 배열.
 *
 * @author Mneme
 * @since phase 17
 */
@RestController
@RequestMapping("/api/graph")
class GraphController(
    private val memoryRepository: MemoryRepository,
    private val memoryLinkRepository: MemoryLinkRepository,
    private val userResolver: AuthenticatedUserResolver,
) {
    /** 전체 그래프. */
    @GetMapping
    fun graph(): GraphResponse {
        val userId = userResolver.currentUserId()
        val memories = memoryRepository.findAllByUserIdAndArchivedAtIsNull(userId)
        val memoryIds = memories.map { it.id }.toSet()
        val nodes =
            memories.map { m ->
                GraphNode(
                    extId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                    title = m.title,
                    byteSize = m.byteSize,
                )
            }
        val edges = mutableListOf<GraphEdge>()
        val broken = mutableListOf<BrokenLink>()
        for (m in memories) {
            val links = memoryLinkRepository.findAllByUserIdAndSourceId(userId, m.id)
            for (link in links) {
                if (link.targetId != null && link.targetId in memoryIds) {
                    edges.add(
                        GraphEdge(
                            sourceExtId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                            targetExtId = PrefixedId(PrefixedId.Prefix.MEMORY, link.targetId!!).format(),
                            label = link.targetLabel,
                        ),
                    )
                } else {
                    broken.add(
                        BrokenLink(
                            sourceExtId = PrefixedId(PrefixedId.Prefix.MEMORY, m.id).format(),
                            targetLabel = link.targetLabel,
                        ),
                    )
                }
            }
        }
        return GraphResponse(nodes = nodes, edges = edges, broken = broken)
    }

    data class GraphNode(
        val extId: String,
        val title: String,
        val byteSize: Int,
    )

    data class GraphEdge(
        val sourceExtId: String,
        val targetExtId: String,
        val label: String,
    )

    data class BrokenLink(
        val sourceExtId: String,
        val targetLabel: String,
    )

    data class GraphResponse(
        val nodes: List<GraphNode>,
        val edges: List<GraphEdge>,
        val broken: List<BrokenLink>,
    )
}
