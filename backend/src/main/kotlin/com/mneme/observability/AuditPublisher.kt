package com.mneme.observability

import com.mneme.id.IdFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 감사 이벤트 발행자.
 *
 * 호출 측 트랜잭션과는 별개로 REQUIRES_NEW 트랜잭션에서 INSERT. 본 phase에선 동기 기록이지만
 * phase 14(observability)에서 비동기 큐로 분리 가능.
 *
 * 본 step에서는 키 이벤트만 활용한다. 다른 이벤트(memory.*, oauth.*)는 해당 phase에서 추가.
 */
@Component
class AuditPublisher(
    private val auditEventRepository: AuditEventRepository,
    private val idFactory: IdFactory,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(
        userId: UUID?,
        actorKind: String,
        action: String,
        targetKind: String? = null,
        targetId: String? = null,
        ip: String? = null,
        userAgent: String? = null,
    ) {
        val event =
            AuditEvent(
                id = idFactory.newUuid(),
                userId = userId,
                actorKind = actorKind,
                action = action,
                targetKind = targetKind,
                targetId = targetId,
                ip = ip,
                userAgent = userAgent,
            )
        auditEventRepository.save(event)
    }
}
