## Hotfix information

A hotfix branch is created off of commit D (latest tag). Work begins on the hot fix and a new commit is added to the repo.
This commit is tagged as a new minor release if you are following this model: http://www.xerxesb.com/2010/12/20/git-describe-and-the-tale-of-the-wrong-commits/
The hotfix is then merged back into the main branch.


Diagram:
```
   A < Merge Commit
   |   \
   |    B tag: hotfix 0.1.1
   C tag: 0.2.0
   |
   D tag: 0.1.0
```


## Release Branch Information:

A release branch is created to run all releases then merged back into master.
For example if you are releasing version 0.2.0
Then you would create a new branch off of commit C and pull your commits into that release branch. Then run a release
and back merge your changes into master.

Diagram:
```
   A < Merge Commit
   |   \
   |    B tag: 0.2.0
   C < Merge Commit
   |      \
   |        D tag: 0.1.0
   E init tag: 0.0.1
```


## Reference Diagrams:

## HotFixes:
#### Nested:
```
A < Merge Commit
|   \
|    B tag: hotfix 0.1.1
C tag: 0.2.0
|
D tag: 0.1.0
```

#### Flat:
```
A Merge
B tag 0.1.1
C tag 0.2.0
D tag: 0.1.0
```

#### Flat First Parent:
```
A Merge
C tag 0.2.0
D tag: 0.1.0
```

## Release Branch:
#### Nested:
```
A < Merge Commit
|   \
|    B tag: 0.2.0
C < Merge Commit
|      \
|        D tag: 0.1.0
E init tag: 0.0.1
```

#### Flat:
```
A Merge
B tag 0.2.0
C Merge
D tag: 0.1.0
E init tag: 0.0.1
```

#### Flat First Parent:
```
A Merge
C Merge
D init tag: 0.0.1
```
