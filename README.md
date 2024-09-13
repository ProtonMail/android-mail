Proton Mail for Android
=======================
Copyright (c) 2024 Proton Technologies AG

## Contributing
We’ve recently rebuilt the app and are focused on our current roadmap of features and fixes. Therefore, we are not accepting new issues or PRs at the moment: feel free to fork the repo for your own experiments. We appreciate your understanding and support.

## Build instructions
- Install and configure the environment (two options available)
  - [Android Studio bundle](https://developer.android.com/studio/install)
  - [Standalone Android tools](https://developer.android.com/tools)
- Install and configure Java 17+ (not needed for Android Studio bundle as it's included)
  - Install Java 17 with `brew install openjdk@17` | `apt install openjdk-17-jdk`
  - Set Java 17 as the current version by using the `JAVA_HOME` environment variable
- Clone this repository (Use `git clone git@github.com:ProtonMail/android-mail.git`.)
- Setup `google-services.json` file by running `./scripts/setup_google_services.sh`
- Build with any of the following:
  - Execute `./gradlew assembleAlphaDebug` in a terminal
  - Open Android Studio and build the `:app` module

## CI / CD
CI stages are defined in the `.gitlab-ci.yml` file and we rely on [fastlane](https://docs.fastlane.tools/) to implement most of them.
Fastlane can be installed and used locally by performing
```
bundle install
```
(requires Ruby and `bundler` to be available locally)
```
bundle exec fastlane lanes
```
will show all the possible actions that are available.

## UI Tests
UI tests are executed on Firebase Test Lab through the CI. UI tests must run on a `dev` flavour (`devDebug` for instance).

While instrumented tests can be run locally with no additional setup, in order to run the tests located in the `app/src/uiTest` folder, some assets (`users.json` and `internal_api.json` for instance) might need to be downloaded and configured.

## Deploy
Each merge to `main` branch builds the branch's HEAD and deploys it
to [Firebase App Distribution](https://firebase.google.com/docs/app-distribution).

## Signing
All `release` builds done on CI are automatically signed with ProtonMail's keystore. In order to perform signing locally, the keystore will need to be placed into the `keystore/` directory and the credentials will be read from `private.properties` file.

## Observability
Crashes and errors that happen in `release` (non debuggable) builds are reported to Sentry in an anonymized form.
The CI sets up the integration with Sentry by providing in the build environment `private.properties` and `sentry.properties` files that contain the secrets needed. 
This can as well be performed locally by creating `private.properties` and `sentry.properties` files and filling them with the needed secrets (eg. `SentryDSN`; for more details about the `sentry.properties` file, see https://docs.sentry.io/platforms/android/gradle/#proguardr8--dexguard).

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
The code and data files in this distribution are licensed under the terms of the GPLv3 as published by the Free Software Foundation. See https://www.gnu.org/licenses/ for a copy of this license.

