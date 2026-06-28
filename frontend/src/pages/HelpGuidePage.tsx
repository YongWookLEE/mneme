import type { JSX } from "react";
import { Link } from "react-router-dom";
import Sidebar from "../components/Sidebar";

interface Section {
  id: string;
  title: string;
  body: JSX.Element;
}

const SECTIONS: Section[] = [
  {
    id: "what-is",
    title: "Mneme란",
    body: (
      <>
        <p>
          여러 AI 클라이언트(Claude·ChatGPT·Codex·Grok 등)가 <strong>같은 영구 메모리</strong>를
          공유하도록 만드는 인프라입니다. ChatGPT에 저장한 내용을 Claude가 못 보던 단절을
          제거하고, 사용자가 자기 데이터의 완전한 소유권·이동권을 갖도록 합니다.
        </p>
        <p>
          저장은 자연어로 "이거 기억해줘"라고 하면 자동 분류·요약·임베딩·태깅이 일어나고,
          검색은 의미(임베딩)와 키워드(전문검색)를 결합한 하이브리드입니다. 본문 안
          <code>[[메모리 제목]]</code> 표기로 메모리 사이를 옵시디언처럼 연결하면 그래프와
          backlink가 자동으로 따라옵니다.
        </p>
        <p className="text-ink-300">
          비상업·셀프호스팅. 약 10명 규모 사용을 목표로 설계됐습니다.
        </p>
      </>
    ),
  },
  {
    id: "concepts",
    title: "핵심 개념",
    body: (
      <>
        <ul className="space-y-2">
          <li>
            <strong>메모리(Memory)</strong> — 마크다운 한 글이 한 메모리입니다. 본문 256KB 상한,
            낙관적 락(version)으로 동시 편집을 안전하게 처리합니다.
          </li>
          <li>
            <strong>폴더(Folder)</strong> — 메모리의 단일 소속 컨테이너. 경로는 슬래시 표기
            (<code>/projects/mneme/</code>). 한 메모리는 정확히 한 폴더에 속합니다.
          </li>
          <li>
            <strong>태그(Tag)</strong> — 한 메모리에 0~16개 자유 입력. 소문자 정규화, 32자 이내.
            LLM이 자동으로 후보를 제안합니다.
          </li>
          <li>
            <strong>위키 링크(<code>[[link]]</code>)</strong> — 본문 안에 <code>[[메모리
            제목]]</code> 또는 <code>[[mem_…]]</code>을 쓰면 백엔드가 자동으로
            <code>memory_links</code> 인덱스를 갱신합니다. 제목이 바뀌면 다른 메모리의
            backlink 본문도 일괄 치환됩니다.
          </li>
          <li>
            <strong>아카이브</strong> — 메모리는 영구 삭제하지 않습니다. <Link to="/archive" className="underline">아카이브</Link>에서 복구 가능.
          </li>
          <li>
            <strong>API 키 / OAuth</strong> — 외부 AI 클라이언트가 본인 데이터에 접근하기 위한
            인증 토큰. 키는 <code>mn_</code> prefix, OAuth는 RFC 7591 DCR로 동적 등록.
          </li>
        </ul>
      </>
    ),
  },
  {
    id: "quick-start",
    title: "빠른 시작",
    body: (
      <ol className="list-decimal space-y-2 pl-5">
        <li>
          상단의 <Link to="/keys" className="underline">키</Link> 페이지에서 새 API 키를
          발급하세요. 평문 키는 발급 직후 1회만 표시됩니다.
        </li>
        <li>
          MCP 클라이언트(예: Codex CLI)에서 <Link to="/connect" className="underline">연결
          가이드</Link>의 스니펫을 복사해서 등록.
        </li>
        <li>
          AI에게 "이거 Mneme에 저장해줘"라고 요청 → 자동으로 폴더가 추론되고 요약·임베딩·
          태그가 생성됩니다.
        </li>
        <li>
          본문 안에 <code>[[연관 메모리 제목]]</code>을 한 줄 추가하면 <Link to="/map"
          className="underline">맵</Link>에서 연결이 보이고 상대 메모리의 backlink 패널에도
          나타납니다.
        </li>
      </ol>
    ),
  },
  {
    id: "pages",
    title: "대시보드 페이지 안내",
    body: (
      <ul className="space-y-2">
        <li><strong>메모리</strong> (<code>/</code>) — 전체 메모리 카드 리스트. 폴더 선택 시 그
          폴더의 메모리만. 폴더 페이지 상단에는 <strong>LLM 합성 폴더 인덱스</strong>가 노출
          되어 주제별 그루핑과 빈 곳 추천을 보여줍니다.</li>
        <li><strong>메모리 상세</strong> (<code>/memory/:id</code>) — 마크다운 뷰. 편집 토글
          시 textarea로 본문 수정. 저장 시 409 충돌이 나면 서버 본문과 내 본문을 좌우로
          보여주고 직접 병합할 수 있습니다. 하단에 피드백 바·backlink 패널.</li>
        <li><strong>검색</strong> (<code>/search?q=</code>) — 벡터(<em>α</em>=0.6) + 전문검색
          (<em>β</em>=0.3) + 트라이그램(<em>γ</em>=0.1) 결합. 상단 검색바 또는
          <kbd>⌘/Ctrl+K</kbd>로 포커스.</li>
        <li><strong>아카이브</strong> (<code>/archive</code>) — 보관된 메모리. 복구 버튼.
          같은 폴더에 동일 제목 활성 메모리가 있으면 복구 실패(409).</li>
        <li><strong>맵</strong> (<code>/map</code>) — 본문 <code>[[link]]</code> 기반 force-
          directed 그래프. 노드 크기는 본문 byteSize. 깨진 링크는 사이드 패널.</li>
        <li><strong>검토(lint)</strong> (<code>/lint</code>) — 약점 감지: 깨진 링크·외톨이·
          미완성(120B 미만)·제목 중복. 각 항목을 클릭하면 해당 메모리로 이동.</li>
        <li><strong>키</strong> (<code>/keys</code>) — API 키 발급/폐기/회전 + Claude
          Desktop·Codex CLI용 연결 명령 빌더(발급 직후 평문 1회 노출).</li>
        <li><strong>연결</strong> (<code>/connect</code>) — Claude Code(CLI), Codex CLI,
          ChatGPT Developer mode용 스니펫 모음 + 복사 버튼. OAuth/API 키 두 방식 모두 안내.</li>
        <li><strong>데이터</strong> (<code>/data</code>) — Export(zip 다운로드,
          manifest.json + memories/*.md frontmatter) + Import(zip 업로드 → preview에서 항목별
          건너뛰기/대체/새로 작성 결정 → apply).</li>
        <li><strong>사용량</strong> (<code>/usage</code>) — 최근 30일 일별 토큰 + 요청 수.
          상단에 요약 카드 4종.</li>
        <li><strong>감사</strong> (<code>/audit</code>) — 본인 보안 활동(키 발급/폐기, 메모리
          archive 등) 최신순. user_id/ip/UA는 응답에 노출하지 않음.</li>
      </ul>
    ),
  },
  {
    id: "mcp-tools",
    title: "MCP 도구 11종",
    body: (
      <table className="w-full table-auto text-sm">
        <thead className="text-xs uppercase text-ink-400">
          <tr>
            <th className="py-1 text-left">이름</th>
            <th className="text-left">기능</th>
          </tr>
        </thead>
        <tbody>
          <tr className="border-t border-ink-700"><td><code>mn_schema</code></td><td>도구 카탈로그 + 서버 메타데이터 + 본문 상한 정보</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_whoami</code></td><td>현재 사용자 ext_id·이메일·locale</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_list</code></td><td>폴더와 메모리 목록 (folder_ext_id, include_archived)</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_read</code></td><td>본문 + 태그 포함 단건 조회</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_search</code></td><td>하이브리드 검색 (folder, tags, limit)</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_write</code></td><td>메모리 생성. 본문 <code>[[link]]</code>도 자동 인덱스</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_update</code></td><td>제목/본문/요약/폴더 갱신. expected_version 필수(낙관적 락)</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_archive</code></td><td>soft delete</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_restore</code></td><td>archived 메모리 복구</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_relations</code></td><td>forward / backlink / 깨진 링크</td></tr>
          <tr className="border-t border-ink-700"><td><code>mn_surface</code></td><td>컨텍스트 힌트로 관련 메모리 추천(검색 wrapper)</td></tr>
        </tbody>
      </table>
    ),
  },
  {
    id: "wiki-link",
    title: "위키 링크 동작",
    body: (
      <>
        <p>
          본문에 <code>[[자바와 비교]]</code>처럼 쓰면 백엔드 <code>WikiLinkParser</code>가
          본문을 정규식으로 훑고(코드 블록은 무시), <code>WikiLinkIndexer</code>가
          <code>memory_links</code> 테이블에 source→target 행을 갱신합니다. 같은 사용자의
          활성 메모리 중 제목이 일치하면 target_id가 채워지고, 없으면 깨진 링크(target_id=null)
          가 됩니다.
        </p>
        <p>
          타겟 메모리 <strong>제목을 바꾸면</strong> <code>BacklinkRenameService</code>가
          다른 메모리 본문에서 <code>[[옛 제목]]</code>을 <code>[[새 제목]]</code>으로
          일괄 치환하고 인덱스도 다시 만듭니다. 사용자가 직접 grep할 필요 없습니다.
        </p>
        <p className="text-ink-300">
          LLM이 본문을 작성·수정할 때도 <code>[[link]]</code>를 자연스럽게 삽입하도록
          시스템 프롬프트에 가이드가 들어가 있습니다.
        </p>
      </>
    ),
  },
  {
    id: "ai-flow",
    title: "메모리 저장 시 LLM 흐름",
    body: (
      <ol className="list-decimal space-y-2 pl-5">
        <li>클라이언트(또는 대시보드)가 본문을 전송 → <code>MemoryWriteFacade.create</code></li>
        <li>본문으로 OpenAI 임베딩(<code>text-embedding-3-small</code>) 1536차원 계산 — 트랜잭션 밖</li>
        <li>요약 생성(<code>gpt-4o-mini</code>) — 트랜잭션 밖. 사용자가 직접 요약을 넘기면 LLM 호출 생략</li>
        <li>토큰 한도 가드(<code>TokenQuotaGuard</code>)가 일일 한도 초과 시 임베딩 skip하고 본문 저장은 계속</li>
        <li><code>MemoryService.create</code>가 본문·메타를 트랜잭션 안에서 저장</li>
        <li><code>MemoryEmbeddingDao</code>가 별도 트랜잭션으로 임베딩 컬럼 갱신</li>
        <li><code>WikiLinkIndexer</code>가 별도 트랜잭션으로 본문 <code>[[link]]</code> 인덱스 갱신</li>
        <li>매일 누적 토큰을 <code>usage_daily</code>에 ON CONFLICT 업서트(<code>PgUsageRecorder</code>)</li>
      </ol>
    ),
  },
  {
    id: "feedback",
    title: "피드백 학습",
    body: (
      <>
        <p>
          메모리 상세 하단의 피드백 바에서 요약·분류·태그·인덱스·전반 5가지 대상에 👍/👎 +
          짧은 메모를 남길 수 있습니다. <code>FeedbackHintBuilder</code>가 다음 LLM 호출의
          시스템 프롬프트 후미에 최근 8건(부정 우선 정렬)을 자동으로 붙여 줍니다.
        </p>
        <p className="text-ink-300">
          별도 학습 잡이나 가중치 모델은 없습니다. 프롬프트 컨텍스트로 신호를 주는 가벼운
          방식이라 즉시 반영되고, 피드백을 지우면 다음 호출부터는 영향이 사라집니다.
        </p>
      </>
    ),
  },
  {
    id: "data-portability",
    title: "데이터 포터빌리티 (Export / Import)",
    body: (
      <>
        <p>
          <Link to="/data" className="underline">/data</Link>에서 zip으로 내보내고 가져옵니다.
          내보내기는 <code>manifest.json</code> + <code>memories/&lt;extId&gt;.md</code>
          (YAML frontmatter 포함). 가져오기는 2단계:
        </p>
        <ol className="list-decimal space-y-1 pl-5">
          <li><strong>Preview</strong> — zip을 파싱해 항목별 제목·폴더·태그·충돌 표시</li>
          <li><strong>Apply</strong> — 항목별 <em>건너뛰기</em>/<em>대체</em>/<em>새로 작성</em>을 선택</li>
        </ol>
        <p className="text-ink-300">
          Mneme zip 외에 일반 마크다운 zip(frontmatter 없거나 첫 # 헤딩이 제목)도 지원합니다.
          zip 디렉터리 구조는 폴더 경로로 매핑됩니다.
        </p>
      </>
    ),
  },
  {
    id: "security",
    title: "보안 정책",
    body: (
      <ul className="space-y-2">
        <li><strong>데이터 격리</strong> — 모든 리포지토리 메서드는 user_id 첫 인자 강제.
          다른 사용자 리소스 접근은 항상 404(존재 여부 미노출).</li>
        <li><strong>API 키</strong> — sha256 해시 + 앞 8자 prefix만 저장. 평문은 발급 응답에 1회.</li>
        <li><strong>OAuth DCR + PKCE</strong> — MCP 클라이언트가 사전 등록 없이 동의 흐름으로 연결.</li>
        <li><strong>Rate limit</strong> — 분당 60·일별 5000·쓰기 분당 20 (env로 변경).
          초과 시 ProblemDetail 429.</li>
        <li><strong>토큰 한도</strong> — 임베딩 일 100k·LLM 일 50k 기본. 초과는 OpenAI 호출 차단.</li>
        <li><strong>PII 로그 마스킹</strong> — <code>mn_*</code>·<code>sk-*</code>·이메일 자동 ✱처리.</li>
        <li><strong>아카이브 only</strong> — 영구 삭제 없음. 계정 삭제만 30일 유예 후 cascade.</li>
        <li><strong>본문 256KB 상한</strong> + <strong>프롬프트 인젝션 펜스</strong>(<code>&lt;&lt;&lt;USER_CONTENT&hellip;END_USER_CONTENT&gt;&gt;&gt;</code> + 8KB 절단).</li>
      </ul>
    ),
  },
  {
    id: "shortcuts",
    title: "단축키",
    body: (
      <ul className="space-y-1">
        <li><kbd>⌘/Ctrl + K</kbd> — 검색바 포커스 (입력 필드 안에서도 동작)</li>
        <li><kbd>Esc</kbd> — 입력 포커스 해제 (브라우저 기본)</li>
      </ul>
    ),
  },
  {
    id: "faq",
    title: "자주 묻는 질문",
    body: (
      <dl className="space-y-3">
        <div>
          <dt className="font-medium text-ink-100">Q. 키를 잃어버렸어요</dt>
          <dd className="text-sm text-ink-300">평문은 발급 직후 한 번만 노출됩니다. 잃어버린
          키는 <Link to="/keys" className="underline">/keys</Link>에서 회전(rotate)으로
          폐기 + 같은 이름으로 새 평문을 발급받으세요.</dd>
        </div>
        <div>
          <dt className="font-medium text-ink-100">Q. 임베딩이 비어 있어요</dt>
          <dd className="text-sm text-ink-300">OpenAI 호출이 실패했거나 일일 토큰 한도를
          넘은 경우입니다. 본문 저장은 계속됩니다. 본문을 한 번 PATCH로 다시 저장하면
          임베딩이 재계산됩니다.</dd>
        </div>
        <div>
          <dt className="font-medium text-ink-100">Q. 폴더 인덱스가 이상해요</dt>
          <dd className="text-sm text-ink-300">폴더 페이지 상단의 인덱스 패널에서 "재생성"
          버튼을 누르면 최신 메모리 목록으로 다시 합성합니다. 피드백(👎 인덱스)을 한 번
          남기면 다음 호출부터 자동 반영.</dd>
        </div>
        <div>
          <dt className="font-medium text-ink-100">Q. 다른 사람 메모리가 보이나요?</dt>
          <dd className="text-sm text-ink-300">아니요. 모든 쿼리가 user_id로 강제 필터링
          되며, 다른 사용자의 ext id를 알아도 응답은 404입니다.</dd>
        </div>
        <div>
          <dt className="font-medium text-ink-100">Q. 외부 클라이언트가 <code>localhost</code>를 못 봅니다</dt>
          <dd className="text-sm text-ink-300">Claude Desktop·Codex CLI는 같은 머신이라
          가능합니다. ChatGPT Developer mode 등 클라우드 클라이언트는 외부에서 접근할
          공개 URL이 필요합니다(예: <code>ngrok http 8080</code>).</dd>
        </div>
        <div>
          <dt className="font-medium text-ink-100">Q. 백업은요?</dt>
          <dd className="text-sm text-ink-300">셀프호스팅에서는 별도 백업 컨테이너가
          매일 03:00 UTC에 pg_dump → 객체 스토리지에 업로드합니다. 상세는
          docs/BACKUP.md를 참고하세요.</dd>
        </div>
      </dl>
    ),
  },
];

/**
 * `/help` — 사용자에게 보여 주는 상세 이용 가이드.
 *
 * 좌측 anchor 목차 + 본문 섹션. 사용자가 어떤 페이지가 어떤 동작을 하는지, 본문 [[link]]가
 * 어떻게 처리되는지, 보안 정책은 어떠한지 한곳에서 파악할 수 있게 한다.
 *
 * @since phase 31 (docs hub)
 */
export default function HelpGuidePage(): JSX.Element {
  return (
    <>
      <Sidebar />
      <main className="flex-1 overflow-auto p-6">
        <div className="mx-auto grid max-w-5xl grid-cols-1 gap-8 md:grid-cols-[200px_1fr]">
          <nav className="hidden md:block">
            <div className="sticky top-4 space-y-1 text-xs text-ink-300">
              <div className="mb-2 font-medium uppercase tracking-wider text-ink-400">목차</div>
              {SECTIONS.map((s) => (
                <a key={s.id} href={`#${s.id}`} className="block rounded px-2 py-1 hover:bg-ink-700 hover:text-ink-100">
                  {s.title}
                </a>
              ))}
            </div>
          </nav>
          <article className="min-w-0 space-y-10">
            <header>
              <h1 className="text-2xl font-medium tracking-tight">사용 가이드</h1>
              <p className="mt-2 text-sm text-ink-300">
                Mneme가 무엇이고, 어떤 페이지가 어떤 동작을 하며, 메모리 저장·검색·연결이
                내부적으로 어떻게 진행되는지 한곳에서 안내합니다.
              </p>
            </header>
            {SECTIONS.map((s) => (
              <section key={s.id} id={s.id} className="scroll-mt-16 space-y-3">
                <h2 className="text-lg font-medium tracking-tight">{s.title}</h2>
                <div className="space-y-2 text-sm leading-relaxed text-ink-200">{s.body}</div>
              </section>
            ))}
            <footer className="border-t border-ink-700 pt-6 text-xs text-ink-400">
              더 깊은 내용은 GitHub 저장소의 <code>docs/</code> 디렉터리를 참고하세요 — PRD,
              ARCHITECTURE, ADR, SELFHOST, BACKUP, TROUBLESHOOTING, SECURITY 등이 있습니다.
            </footer>
          </article>
        </div>
      </main>
    </>
  );
}
