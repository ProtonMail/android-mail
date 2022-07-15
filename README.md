ProtonMail for Android
=======================
Copyright (c) 2021 Proton Technologies AG

## Build instructions
- Install and configure environment (two options available)
    - [Android studio bundle](https://developer.android.com/studio/install) (Please use at least version 2021.1.1 "Bumblebee")
    - [Standalone android sdk](https://android-doc.github.io/sdk/installing/index.html?pkg=tools)
- Install and configure Java 11 (not needed for android studio bundle as it's included)
    - Install java11 with `brew install java11` | `apt install openjdk-11-jdk`
    - Check installed versions with `/usr/libexec/java_home -V`
    - Set java 11 as the current version with `export JAVA_HOME=`/usr/libexec/java_home -v 11.x.x`
- Clone this repository with submodules (Use `git clone --recurse-submodules [url]`.).
    - If the project was already cloned use `git submodule init` and then `git pull --recurse-submodules`
- Build with any of the following: Execute `./gradlew assembleDebug`, open and build in Android Studio or use fastlane as defined below to build

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

## UI Tests
UI tests are executed on firebase through the CI. Firebase test lab can be triggered also locally with `bundle exec fastlane uiTests` or tests can be run in a local emulator through android studio.
UI tests must run on a `dev` flavour ( like `devDebug` ). 
The `app/src/uiTest/assets/users.json` and  `app/src/uiTest/assets/internal_api.json` files will be needed and their value can be found in confluence or in the CI env vars.
**Currently, only the ui tests that are included in the `SmokeSuite` class are run on firebase**


## Deploy
Each merge to `master` branch builds the branch's HEAD and deploy it to [firebase app distribution](https://firebase.google.com/docs/app-distribution)
In order to someone as a tester for such builds, their email address needs to be added to the `v6-internal-alpha-testers` group in Firebase.

## Signing
All `release` builds done on CI are automatically singed with ProtonMail's keystore. In order to perform signing locally the keystore will need to be placed into `keystore/` directory and the credentials will be read from `private.properties` file.


## Observability
Crashes and errors that happen in `release` (non debuggable) builds are reported to Sentry in an anonymised form.
The CI sets up the integration with Sentry by providing in the build environment a `private.properties` file that contains the secrets needed. This can as well be performed locally by creating a `private.properties` file (which will be ignored by git) and filling it with the needed secrets (eg. `SentryDSN`)


## Use core libraries from local git submodule
It is possible to run the application getting the "core" libraries from the local git submodule instead of gradle by setting the following flag to true in `gradle.properties` file:

```
useCoreGitSubmodule=true
```


## Code style
This project's code style and formatting is checked by detekt. The rule set is [ktlint's default one](https://github.com/pinterest/ktlint)


## Troubleshooting
- `goopenpgp.aar` library not found: submodule not properly setup, please follow steps in build instructions

License
-------
The code and datafiles in this distribution are licensed under the terms of the GPLv3 as published by the Free Software Foundation. See https://www.gnu.org/licenses/ for a copy of this license.

