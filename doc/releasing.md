# Releasing

1. Update the version number in these places:

   - [project.clj](../project.clj)

1. Update [CHANGELOG.md](./CHANGELOG.md). We follow the guidelines from
   [keepachangelog.com](http://keepachangelog.com/) and [Semantic
   Versioning](http://semver.org/)

1. Commit changes, create a PR, merge the PR into master.

1. Create a signed tag at the release commit: `git tag -s X.X.X -m "X.X.X
   Release" && git push origin X.X.X`

   This will automatically create a release on GitHub.

1. Deploy the library to Clojars: `lein deploy clojars`

   You will need to be a member of the `amperity` organization on Clojars to
   push artifacts.
