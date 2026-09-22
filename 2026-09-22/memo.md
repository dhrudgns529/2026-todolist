# 원본 엑셀 → 전처리 → CSV → 버킷 업로드 과정 (board-inquiry-router)

## STEP 0. 폴더 준비 (한 번만)

```
C:\work\voc\excel     ← 원본 엑셀 파일들을 여기에 넣는다
C:\work\voc\parquet   ← (자동 생성됨, 손댈 필요 없음)
C:\work\voc\out       ← (자동 생성됨, 손댈 필요 없음)
```

## STEP 1. 값(env) 설정

`.env` 파일에 아래 3개만 채운다 (업로드용):
```
DATAHUB_BASE_URL=<버킷 플랫폼 주소>
BUCKET_ID=<업로드할 버킷 ID>
DOC_OWNER_ID=<문서 소유자 ID>
```
전처리 단계(1~3)는 이 값이 없어도 돌아간다. 오직 마지막 업로드 단계에서만 필요하다.

콘솔(PowerShell) 창에서 매번:
```powershell
$env:PYTHONUTF8 = "1"
$env:UV_NATIVE_TLS = "1"
cd C:\work\board-inquiry-router
```

## STEP 2. 명령 순서대로 실행

| # | 하는 일 | 명령 |
|---|---|---|
| ① | 엑셀 → parquet 변환 | `uv run --native-tls python -m lab.analysis.convert --in C:\work\voc\excel --out C:\work\voc\parquet --force` |
| ② | 전처리 실행 + 후보 비교 | `uv run --native-tls python -m lab.demo.analyze --raw-parquet C:\work\voc\parquet --out-dir C:\work\voc\out` |
| ③ | ②의 결과 보고서를 보고 전처리 방식 하나 고르기 | `C:\work\voc\out\analyze\html\preset_compare.html` 을 브라우저로 열어서 눈으로 확인 → preset 이름 확인 (예: `D-KEY`) |
| ④ | 고른 전처리로 CSV 생성 + 버킷 업로드 | `uv run --native-tls python -m lab.demo.publish --raw-parquet C:\work\voc\parquet --preset D-KEY --out-dir C:\work\voc\out --send` |

- ④의 `--preset D-KEY`는 ③에서 직접 고른 이름으로 바꿔 넣는다.
- `--send`를 빼면 CSV까지만 만들고 업로드는 하지 않는다(테스트용).

## 결과물이 나오는 위치

```
C:\work\voc\out\upload\datahub_export_*.csv   ← 최종 CSV
C:\work\voc\out\upload\manifest.json          ← 어떤 전처리로 만들었는지 기록
C:\work\voc\out\upload\upload.sqlite          ← 업로드 결과 기록
```

## 성공 확인

- ①: `converted=N skipped=0 failed=0` 문구 확인
- ④: 콘솔에 `upload skipped reason=...`가 뜨면 → STEP 1의 `.env` 3개 값이 비어 있다는 뜻. 이 문구 없이 끝나면 업로드 성공.
