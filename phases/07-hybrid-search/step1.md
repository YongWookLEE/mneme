# step 1 — search-infra-check

기존 인덱스 활용 가능 여부 확인. V1__init.sql에서:
- `memories_embedding_idx ivfflat vector_cosine_ops lists=100`
- `memories_tsv_idx gin(tsv)` + trigger `tsvector_update_trigger`
- `memories_content_trgm_idx gin(content gin_trgm_ops)`

본 step은 코드 없이 SQL EXPLAIN 한 번 돌려서 인덱스 사용 확인.
