#!/bin/bash
FILE=$1
FAILED=$(grep -c -F "Failures: 0" $FILE)
ERRORS=$(grep -c -F "Errors: 0" $FILE)
RC2=$?
if (( (RC1 + RC2) > 0)); then
  echo "RC1=$RC1, RC2=$RC2: $FILE"
  cat $FILE
fi
