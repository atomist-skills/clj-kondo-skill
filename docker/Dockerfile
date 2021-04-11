FROM gcr.io/atomist-container-skills/clojure-base:0.0.1@sha256:af8bdf2f0d75b91b95bfb66c542ee7309442def90b1ec47fe0a6c05f90338784

WORKDIR /usr/src/app

COPY --from=borkdude/clj-kondo:2020.06.21 /usr/local/bin/clj-kondo /usr/local/bin/clj-kondo

COPY package*.json /usr/src/app/
RUN npm ci
COPY deps.edn shadow-cljs.edn /usr/src/app/
COPY src /usr/src/app/src
RUN npm run build

ENTRYPOINT [ "node" ]
CMD [ "index.js" ]
