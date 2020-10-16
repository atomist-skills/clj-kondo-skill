# Copyright © 2020 Atomist, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

cat << END > "/Users/slim/atomist/atomisthq/bot-service/payload.json"
{
  "extensions": {
    "correlation_id": "corrid1",
    "team_id": "T095SFFBK"
  },
  "data": {
    "Push": [
      {"after": {
         "sha": "b2f284f74fcf8f9fb359e6d2144b9abe00bb02aa"},
       "branch": "linting-fixes",
       "repo": {
         "owner": "atomisthq",
         "name": "bot-service",
         "channels": [{
           "name": "bot-service",
           "channelId": "CDU23TC1H",
           "team": {
             "id": "T095SFFBK",
             "name": "atomist (prod)"}}]}}]},
  "secrets": [{"uri": "atomist://api-key", "value": "'"$API_KEY_PROD"'"}]}
END

docker run -it \
	-v "/Users/slim/atomist/atomisthq/bot-service:/atm/home" \
	-v "/Users/slim/atmhq/package-cljs-skill:/atm/secret" \
  -e ATOMIST_WORKSPACE_ID=T095SFFBK \
  -e GRAPHQL_ENDPOINT=https://automation.atomist.com/graphql \
  -e ATOMIST_PAYLOAD=/atm/home/payload.json \
  -e ATOMIST_HOME=/atm/home \
  -e ATOMIST_STORAGE=gs://ak748nqc5-workspace-storage \
  -e GOOGLE_APPLICATION_CREDENTIALS=/atm/secret/atomist-skill-production-ec3c6e5c9a1b.json \
  -e ATOMIST_CORRELATION_ID=corrid \
  -e ARTIFACTORY_USER=$ARTIFACTORY_USER \
  -e ARTIFACTORY_PWD=$ARTIFACTORY_PWD \
  -e TOPIC=projects/atomist-skill-staging/topics/packaging-test-topic \
  gcr.io/atomist-container-skills/clj-kondo-skill:d1628651b2792814454c971b243e284244823399
