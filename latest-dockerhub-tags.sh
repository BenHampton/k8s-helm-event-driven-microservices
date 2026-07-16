#!/usr/bin/env bash
# Show the newest image tag on Docker Hub for each service, next to what
# values-prod.yaml currently pins. Run from the repo root.
#
#   ./scripts/latest-dockerhub-tags.sh           # show
#   ./scripts/latest-dockerhub-tags.sh --bump    # rewrite values-prod.yaml to the newest
#
# Skips `latest` — that's dev's tag, and pinning a moving target in prod is the
# thing this project exists to argue against.

set -euo pipefail

USER="${DOCKERHUB_USER:-bhampton29}"
SERVICES=(order-service notification-service ui)
BUMP=false
[[ "${1:-}" == "--bump" ]] && BUMP=true

command -v jq >/dev/null || { echo "needs jq: brew install jq"; exit 1; }

printf '%-22s %-12s %-12s %s\n' SERVICE PINNED LATEST ""
printf '%-22s %-12s %-12s %s\n' ---------------------- ------------ ------------ ------

for svc in "${SERVICES[@]}"; do
  values="./${svc}/helm/values-prod.yaml"
  [[ -f "$values" ]] || { echo "no $values — run from the repo root"; exit 1; }

  # the tag currently pinned
  pinned=$(grep -E '^\s+tag:' "$values" | head -1 | sed -E 's/.*tag:\s*"?([^"#]+)"?.*/\1/' | xargs)

  # newest tag that isn't `latest`, by last_updated
  latest=$(curl -fsSL \
    "https://hub.docker.com/v2/repositories/${USER}/${svc}/tags?page_size=100&ordering=last_updated" \
    | jq -r '[.results[] | select(.name != "latest")] | .[0].name // "none"')

  if [[ "$pinned" == "$latest" ]]; then
    mark="current"
  elif [[ "$latest" == "none" ]]; then
    mark="no SHA tags pushed"
  else
    mark="behind"
    if $BUMP; then
      # macOS and GNU sed disagree about -i; this works on both
      sed -E "s|(^[[:space:]]+tag:[[:space:]]*)\"?${pinned}\"?|\1\"${latest}\"|" "$values" > "$values.tmp"
      mv "$values.tmp" "$values"
      mark="bumped -> $latest"
    fi
  fi

  printf '%-22s %-12s %-12s %s\n' "$svc" "$pinned" "$latest" "$mark"
done

if $BUMP; then
  echo
  echo "values-prod.yaml files rewritten. Review, then:"
  echo "  git diff"
  echo "  git commit -am 'promote to prod' && git push"
  echo
  echo "Argo picks it up on the next sync — that commit IS the deploy."
fi