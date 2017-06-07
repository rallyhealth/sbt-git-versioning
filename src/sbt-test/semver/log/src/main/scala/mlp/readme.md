There are several "Equestria" files here each one containing a single
class -- in fact the same class -- to test semver behavior.

The "real" file is Equestria.scala. The other files are intended to be
copied over the "Equestria.scala" and MUST have their class renamed to just
"Equestria" by "checkSemver.sh". (They can't have the same class name
or the compiler would complain.)
