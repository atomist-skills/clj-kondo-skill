FROM borkdude/clj-kondo:latest AS binary

FROM node:10-slim

COPY --from=binary /usr/local/bin/clj-kondo /usr/local/bin/clj-kondo

ENTRYPOINT [ "node" ]
CMD [ "index.js" ]
