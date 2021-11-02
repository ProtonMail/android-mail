ProtonMail for Android
=======================
Copyright (c) 2021 Proton Technologies AG

## Build instructions
- [Install android sdk](https://android-doc.github.io/sdk/installing/index.html?pkg=tools)
- Clone this repository (Use `git clone --recurse-submodules [url]`.).
- Execute `./gradlew assembleDebug`, open and build in Android Studio or use fastlane as defined below to build

## CI / CD
CI stages are defined in the `.gitlab-ci.yml` file and we rely on [fastlane](https://docs.fastlane.tools/) to implement most of them.
Fastlane can be installed and used locally by performing
```
bundle install
```
(requires Ruby and bundler to be available locally)
```
bundle exec fastlane lanes
```
will show all the possible actions that are available.

### Master is continuously deployed
Each merge to `master` branch builds the branch's HEAD and deploy it to [firebase app distribution](https://firebase.google.com/docs/app-distribution)
In order to someone as a tester for such builds, their email address needs to be added to the `v6-dev-builds-testers` group in Firebase.


## Code style
This project's code style and formatting is checked by detekt. The rule set is [ktlint's default one](https://github.com/pinterest/ktlint)


License
-------
The code and datafiles in this distribution are licensed under the terms of the GPLv3 as published by the Free Software Foundation. See https://www.gnu.org/licenses/ for a copy of this license.

