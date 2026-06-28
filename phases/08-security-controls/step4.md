# step 4 — isolation-rest

REST 전수 회귀 테스트. 두 사용자 A, B에 대해:
1. A의 폴더/메모리/태그/키 ID로 B의 토큰으로 GET/PATCH/DELETE/POST 호출 → 404.
2. /api/search에서 A의 데이터가 B 결과에 안 섞임.

@SpringBootTest + 직접 DB 시드(api_keys SQL insert) + RestAssured/MockMvc.
