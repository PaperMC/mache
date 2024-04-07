set +e
# $1 is version
# $2 is the "action"
# $3 is the job url
gh issue list --state open --label update --json title | jq '.[].title' | cut -c2- | rev | cut -c2- | cut -d' ' -f1 | rev | grep "$1"
if [[ "$?" -ne "0" ]]; then
  set -e
  gh issue create \
    --title "Failed to $2 for $1" \
    --body "Failed to $2 for $1. Please check the [logs]($3) for more information. The automatic updating will be paused until this is closed" \
    --label update
fi
