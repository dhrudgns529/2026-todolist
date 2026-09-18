# 헬름 차트 새로운 버전 생성 가이드

## 개요
DevOps Console 플랫폼을 통해 헬름 차트의 새 버전을 만들고, 배포까지 진행하는 전체 절차를 정리한 문서입니다.

---

## 1. 기존 차트 다운로드

1. DevOps Console 접속 (url: 추후 기재)
2. **저장소 > 헬름차트 > 프로젝트** 이동
3. 최신 버전 선택 후 다운로드 (`.tgz` 파일)

---

## 2. 로컬에서 차트 수정

### 2-1. 압축 해제
```bash
tar -xzvf mychart-{version}.tgz
```
압축을 풀면 아래와 같은 구조가 나옵니다.
```
mychart/
├── Chart.yaml
├── values.yaml
├── .helmignore
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── configmap.yaml
    ├── secret.yaml
    ├── hpa.yaml
    ├── _helpers.tpl
    └── tests/
```

### 2-2. 버전 수정 — `Chart.yaml`
```yaml
apiVersion: v2           # Helm 차트 스펙 버전, 특별한 이유 없으면 그대로 둠
name: mychart
version: 1.0.0           # 차트 자체 버전 — 이번에 새로 올릴 값
appVersion: "2.0.0"      # 배포되는 앱 자체의 버전 — 앱 릴리즈 태그에 맞춰 수정
```
- `version`은 semver(`MAJOR.MINOR.PATCH`) 규칙을 따름
  - PATCH: 사소한 값 수정
  - MINOR: 기능/설정 추가 (하위 호환 유지)
  - MAJOR: 로직 자체가 크게 바뀌는 breaking change
- `apiVersion`은 Helm 3 기준 `v2` 고정, 건드릴 필요 없음

### 2-3. 값 수정 — `values.yaml`
환경변수/설정값은 `configmap.data`, `secret.data` 형태로 관리됩니다.
```yaml
configmap:
  data:
    SAMPLE_ENDPOINT: ""
    APP_ENV: "production"

secret:
  data:
    SAMPLE_API_KEY: ""
```
- `configmap.data`: 평문으로 노출되어도 되는 설정값 (endpoint, timeout, 옵션 값 등)
- `secret.data`: 민감정보 (API 키, DB 비밀번호 등) — **평문으로 넣어도 됨**, 템플릿에서 자동 인코딩 처리됨 (아래 참고)
- 값이 비어있으면 해당 의존성이 mock으로 폴백되는 구조라면, 실제 배포 전 빈 값이 없는지 재확인 필요

### 2-4. Secret 템플릿 확인 — `templates/secret.yaml`
Secret은 base64 인코딩이 필요하므로 아래처럼 파이프라인 함수가 적용되어 있어야 합니다.
```yaml
data:
  {{- range $key, $value := .Values.secret.data }}
  {{ $key }}: {{ $value | toString | b64enc | quote }}
  {{- end }}
```
- `toString`: 값 타입을 문자열로 통일 (숫자/불리언으로 잘못 파싱되는 것 방지)
- `b64enc`: base64 인코딩 (values.yaml에는 평문 그대로 넣으면 자동 인코딩됨)
- `quote`: 인코딩 결과를 YAML 상 문자열로 안전하게 감쌈

ConfigMap(`templates/configmap.yaml`)은 평문 저장이므로 `b64enc` 적용하지 않음.

---

## 3. Helm 설치 및 패키징 (Windows 기준)

### 3-1. Helm 설치 (최초 1회)
```powershell
winget install Helm.Helm
```
설치 확인:
```powershell
helm version
```

### 3-2. 수정 내용 검증
```bash
helm lint mychart/
helm template mychart/                              # 전체 렌더링 미리보기
helm template mychart/ --show-only templates/secret.yaml     # secret 인코딩 확인
```

### 3-3. 패키징
```bash
helm package mychart/
```
→ `Chart.yaml`의 `name` + `version` 기준으로 `mychart-{version}.tgz` 생성됨

이 단계까지 완료하면 **새로운 헬름 차트 tar 파일 생성 완료**.

---

## 4. DevOps Console에 업로드 및 버전 등록

1. **저장소 > 차트 저장소 > 업로드**
   - 위에서 만든 `.tgz` 파일 업로드
   - 업로드 후 목록에서 새 버전 생성된 것 확인 가능
2. **저장소 > 헬름차트 > 프로젝트 > 버전 추가**
   - 버전 추가 메뉴 진입 후 프로세스 따라 입력
   - 입력값은 이전 버전의 상세정보를 참고하면 대부분 동일하게 채우면 됨

---

## 5. 빌드/배포

1. **빌드/배포 > 헬름 인스톨 > 프로젝트**
   - 방금 추가한 버전을 선택해서 Helm Install 진행
   - 개발/운영 환경, 릴리스명, 네임스페이스 입력
2. **빌드/배포 > Kubernetes 배포**
   - 입력한 릴리스명으로 배포가 생성된 것 확인

---

## 요약 흐름
```
DevOps Console에서 최신 버전 다운로드
   ↓
로컬에서 압축 해제 → Chart.yaml(버전) / values.yaml(설정값) / secret.yaml(인코딩 로직) 수정
   ↓
helm lint / helm template 로 검증
   ↓
helm package 로 재패키징 (.tgz)
   ↓
DevOps Console 차트 저장소에 업로드
   ↓
헬름차트 프로젝트에 새 버전 등록
   ↓
헬름 인스톨로 배포 (릴리스명 / 네임스페이스 지정)
   ↓
Kubernetes 배포 화면에서 릴리스 생성 확인
```
