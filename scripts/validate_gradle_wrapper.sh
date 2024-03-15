#!/bin/bash

#
# Copyright (c) 2022 Proton Technologies AG
# This file is part of Proton Technologies AG and Proton Mail.
#
# Proton Mail is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# Proton Mail is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
#

WRAPPER_DIR="./gradle/wrapper"
WRAPPER_PROPERTIES="gradle-wrapper.properties"
REGEX="gradle-([0-9.]+)-.*.zip"
MATCH_FOUND=false

# This scripts verifies the integrity of the Gradle Wrapper.
# https://docs.gradle.org/current/userguide/gradle_wrapper.html#manually_verifying_the_gradle_wrapper_jar
#
# If you have updated the wrapper and the CI pipeline is failing, please make sure to properly update the wrapper with
# ./gradlew wrapper --gradle-version <YOUR_VERSION>
# from the project root folder rather than just manually changing the version in gradle-wrapper.properties.

function match_wrapper_sha256() {
  curl -L -s -o gradle-wrapper.jar.sha256 \
        https://services.gradle.org/distributions/gradle-"$1"-wrapper.jar.sha256

  # Leave the leading space here, it's intentional
  echo " gradle-wrapper.jar" >> gradle-wrapper.jar.sha256

  sha256sum --check gradle-wrapper.jar.sha256
}

function verify_gradle_wrapper_integrity() {
  cd $WRAPPER_DIR || exit 1

  while IFS= read -r line
  do
    if [[ $line =~ $REGEX ]]; then
      GRADLE_VERSION=${BASH_REMATCH[1]}
      MATCH_FOUND=true

      printf "Gradle version detected: %s\n" "$GRADLE_VERSION"
      match_wrapper_sha256 "$GRADLE_VERSION"
    fi
  done < "$WRAPPER_PROPERTIES"

  if [ $MATCH_FOUND == "false" ]; then
    printf "Unable to match Gradle version.\n"
    exit 1
  fi
}

verify_gradle_wrapper_integrity
