## 현재 상황
- 사용 중인 브랜치(기준 브랜치, 모두 원격에 있음):
  origin/main, origin/feat/itc-judge-init, origin/itvoc-ai-router,
  origin/itvoc-ai-router-legacy, origin/kwh-local-judge
- 분석 대상: 위 5개, origin/HEAD, chore/branch-audit 을 제외한 모든 원격 브랜치
- 목표: 분석 대상 중 기준 브랜치에 이미 포함된 "과거 브랜치"를 뽑고,
  나머지는 기준 브랜치에 없는 코드가 무엇인지 분석한다. 필요한 코드를 놓치지 않는 것이 최우선이다.
- 이 환경은 명령마다 사용자 승인이 필요하다.

## 0단계: 권한 요청 계획 (아무것도 실행하기 전에 먼저)
어떤 명령도 실행하지 말고, 아래 형식으로 승인이 필요한 작업을 모두 보여준 뒤 내 확인을 기다린다.

  [A. 조회 명령] 사용할 git 명령 목록 (fetch, for-each-ref, rev-parse, rev-list, cherry,
      merge-tree, merge-base, diff, log, grep, cat-file, status 등)
  [B. 작업 파일] ../branch-audit-work/ 폴더를 만들고, 분석 스크립트와 중간 결과를 이 폴더에 작성
  [C. 스크립트 실행] bash ../branch-audit-work/scan.sh 로 분석 실행 (단계별 1~2회)
  [D. 커밋/푸시] git switch -c, 결과 파일 복사, git add, git commit, git push -u, 원래 브랜치로 git switch

- 승인 요청을 줄이기 위해, 분석 명령을 하나씩 실행하지 말고 스크립트 파일로 묶어 한 번에 실행한다.
- 목록에 없는 명령이 필요해지면 실행하지 말고 멈춰서 이유와 함께 먼저 묻는다.
- D단계는 분석 결과를 보고한 뒤 다시 한 번 확인받고 진행한다.

## 규칙
- 분석 중에는 조회 명령만 사용한다. (브랜치 삭제, reset, rebase 금지)
- 레포 안 파일은 D단계 전까지 만들거나 수정하지 않는다. 작업 파일은 ../branch-audit-work/ 에만 둔다.
- 오래됐거나 behind가 크다는 이유만으로 과거 브랜치로 분류하지 않는다.
- 판단이 애매하면 과거 브랜치로 분류하지 말고 분석 대상으로 남긴다.

## 방법
### 1단계: 과거 브랜치 판별
시작할 때 git fetch --all --prune 을 실행한다. 기준 브랜치 5개가 있는지, 분석 대상이 몇 개인지 보고한다.
분석 대상 X를 기준 브랜치 A 각각과 비교한다. 아래 중 하나라도 참이면 과거 브랜치로 분류하고, 포함된 A를 기록한다.
1. git rev-list --count A..X == 0  → X의 모든 커밋이 A에 있음
2. git cherry A X 결과에 '+' 줄이 없음  → 같은 패치가 A에 있음 (cherry-pick, rebase)
3. git merge-tree --write-tree A X 가 충돌 없이 성공하고, 결과 트리가 git rev-parse "A^{tree}" 와 같음
   → 내용이 이미 A에 반영됨 (squash 병합 등). git 2.38 미만이면 생략하고 그 사실을 보고한다.
1단계 결과(개수 요약)를 보고한 뒤 2단계로 진행한다.

### 2단계: 나머지 브랜치 상세 분석
- 기준 선택: 5개 A 각각에 대해 git cherry A X 의 '+' 개수를 세고, 가장 적은 A를 비교 기준으로 삼는다.
- 중복 제거: 분석 대상 X가 다른 분석 대상 Y에 포함되면(git merge-base --is-ancestor X Y)
  Y만 분석하고 X는 "Y에 포함됨"으로 기록한다.
- 고유 커밋: git cherry -v A X 의 '+' 커밋 (해시, 날짜, 작성자, 메시지)
- 파일 변경: git diff --name-status A...X
  파일마다 A에 현재 존재하는지, A에서 이후 삭제되거나 이름이 바뀌었는지 확인한다.
- 줄 단위 반영 확인:
  git diff A...X 에서 추가된 줄(+)을 뽑고, 공백과 의미 없는 줄({, }, 빈 줄, 단순 import)은 제외한다.
  남은 줄을 5개 기준 브랜치 전체에서 git grep -F 로 검색해 파일마다 판정한다.
  · 반영됨: 모두 존재
  · 일부 반영: 일부만 존재 → 없는 줄을 보여준다
  · 고유: 대부분 없음 → 해당 코드 블록을 보여준다
- 요약: 브랜치의 목적 2~3줄 (추측은 "추측"으로 표시), 현재 코드와 충돌하거나 이미 대체됐는지

## 결과물 (먼저 ../branch-audit-work/ 에 작성)
- README.md: 조사 일시, 기준 브랜치별 커밋 해시, 판정별 개수,
  "잃으면 안 되는 고유 코드" 목록(브랜치 / 파일 / 설명, 고유 코드가 많은 순),
  분석에 실패한 브랜치
- past-branches.md: | 브랜치 | 포함된 기준 브랜치 | 근거(1/2/3) | 마지막 커밋일 | 작성자 |
- diff-branches.md: 브랜치당 한 섹션으로 비교 기준, 고유 커밋, 파일 표
  (| 경로 | 변경유형 | 기준에 존재 | 반영 여부 | 비고 |), 고유 코드 조각, 요약

## 결과물 커밋 (D단계: 재확인 후 진행)
1. git status 로 수정 중인 파일이 없는지 확인한다. 있으면 멈추고 보고한다.
2. 현재 브랜치 이름을 기록한다.
3. git switch -c chore/branch-audit origin/main
   (이 브랜치가 로컬이나 원격에 이미 있으면 멈추고 나에게 묻는다)
4. ../branch-audit-work/ 의 md 파일 3개를 branch-audit/ 폴더로 복사한다.
5. git add branch-audit/
   git commit -m "docs: add branch audit report"
   git push -u origin chore/branch-audit
6. 2번에서 기록한 원래 브랜치로 돌아간다.
7. 커밋 해시, 판정별 개수, 고유 코드가 가장 많은 브랜치 상위 5개를 보고한다.
