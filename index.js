var {withCheckRunSendingLinter} = require("@atomist/api-cljs/atomist.middleware");
var {onError} = require("@atomist/api-cljs/atomist.clj_kondo");

// free to construct this in various ways - use config to merge local data, grab gists, etc.
var constructArgs = (context) => {
  ["--lint", "./src", "--config", '{:output {:format :json}}'];
};

// more testable?
var onSuccess = (err, stdout, stderr) => {
  return {
    conclusion: "success",
    output: {
      title: "clj-kondo saw no warnings or errors",
      summary: stdout}};
};

/** 
 * 3 extension points
*/
exports.handler = function() {
  withCheckRunSendingLinter({
    cmd: "/usr/local/bin/clj-kondo",
    ext: ".clj",
    constructArgs,
    onSuccess,
    onError
  });
}

exports.handler();