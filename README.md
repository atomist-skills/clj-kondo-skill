# `@atomist/clj-kondo-skill`

<!---atomist-skill-readme:start--->

> "A linter for Clojure code that sparks joy."

Lint your clojure code using [clj-kondo][clj-kondo], get GitHub CheckRuns with warnings and errors.
  
# What it's useful for

Keep track of lint warnings and errors across your repositories.

* Run `clj-kondo` with the same consistent configuration across all of your Clojure code
* Share `clj-kondo` configurations with other teams using GitHub gists.
* Distinguish new warnings and violations from ones that were already present.

# Before you get started

The **GitHub** integration must be configured in order to use this skill. 
At least one repository must be selected. 

# How to configure

1.  **Optionally choose the name of the CheckRun**

    ![check-name](docs/images/check-name.png)
    
2.  **Choose a clj-kondo configuration**

    Optionally, customize the configuration that `clj-kondo` will use by adding a config here. 

    The [clj-kondo configuration documentation][configuration] outlines different ways to control how the code
    is formatted.  To encourage sharing, you can also reference a public gist that contains a great
    clj-kondo config that matches your needs.  Instead of adding the config `edn` document here, 
    users can just reference a public url (e.g. a gist [url like this][gist-url]).
    
    ![config](docs/images/config.png)
                        
3.  **Select repositories**

    By default, this skill will be enabled for all repositories in all organizations you have connected. To restrict 
    the organizations or specific repositories on which the skill will run, you can explicitly 
    choose organization(s) and repositories.

    Either select all, if all your repositories should participate, or choose a subset of repositories that should 
    stay formatted.  This skill will take no action on repositories that do not contain `.clj`, `.cljs`, or `cljc` files.
    
    ![repo-filter](docs/images/repo-filter.png)    

# How to Use

1. **Configure the skill as described above**

1. **Commit and push your code changes** 

1. **See clj-kondo results in your GitHub CheckRun statuses!**

There are already great ways to integrate [clj-kondo][clj-kondo] into your local development flow.  See the docs on
[editor integration here][editor-integration].  However, this skill pulls this data into your release flow and allows
you to build new ways of deciding whether code should be released.

To create feature requests or bug reports, create an [issue in the repository for this skill](htt
ps://github.com/atomist-skills/clj-kondo-skill/issues). 
See the [code](https://github.com/atomist-skills/clj-kondo-skill) for the skill.

[clj-kondo]: https://github.com/borkdude/clj-kondo
[configuration]: https://github.com/borkdude/clj-kondo/blob/master/doc/config.md
[editor-integration]: https://github.com/borkdude/clj-kondo/blob/master/doc/editor-integration.md
[gist-url]: https://gist.githubusercontent.com/slimslenderslacks/574326df04e63527f54b4d0bb9b962d9/raw/8e0e3e6a691c434a63b661ae3869dd181c4fbb89/config.edn

<!---atomist-skill-readme:end--->

---

Created by [Atomist][atomist].
Need Help?  [Join our Slack workspace][slack].

[atomist]: https://atomist.com/ (Atomist - How Teams Deliver Software)
[slack]: https://join.atomist.com/ (Atomist Community Slack)