#!/bin/sh

set -e
set -u
set -o pipefail

command="$1"
shift

host="localhost"
port="8080"

curl_statement="curl --show-error --silent --write-out '\n%{http_code}\n' -X POST --url 'http://${host}:${port}/${command}'"

while [ $# -gt 0 ]; do
    curl_statement="${curl_statement} --data '$1'"
    shift
done

curl_output="$(eval "${curl_statement}" || true)"

response_body="$(echo "${curl_output}" | sed '$d')"
http_status_code="$(echo "${curl_output}" | tail -n 1)"

case "${http_status_code}" in
    2??)
        echo "${response_body}"
        ;;
    *)
        echo "${response_body}" >&2
        exit 1
        ;;
esac
