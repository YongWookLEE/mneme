# step 3 — search-rest

`GET /api/search?q={text}&folderExtId=&tags=tag1,tag2&from=ISO&to=ISO&limit=20`.

응답: `[{ extId, folderExtId, title, summary, score, createdAt, updatedAt }]`. 본문은 length 길어 응답 제외(상세 조회는 `/api/memories/{id}`).

Empty query → 400. limit 1..100 clamp.
