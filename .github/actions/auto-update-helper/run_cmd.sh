set +e
eval $1
exitcode="$?"
echo "exitcode=$exitcode" >> $GITHUB_OUTPUT
exit "$exitcode"
