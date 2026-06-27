package com.mneme.observability

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * 감사 이벤트 리포지토리.
 *
 * 본인 활동 조회는 `findAllByUserIdOrderByCreatedAtDesc`. 관리자용 전수 조회는 본 phase 외 영역.
 */
@Repository
interface AuditEventRepository : JpaRepository<AuditEvent, UUID> {
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<AuditEvent>
}
