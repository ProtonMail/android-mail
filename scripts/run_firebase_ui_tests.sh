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

#!/bin/bash

set -e

case $1 in

'smoke-test')
  device_config=smokeTest
  shards=2
  flaky_attempts=1
  test_targets='filter ch.protonmail.android.uitest.filters.SmokeTestFilter'
  timeout=15m
  ;;

'full-regression-test')
  device_config=fullTest
  shards=4
  flaky_attempts=1
  test_targets='filter ch.protonmail.android.uitest.filters.FullRegressionTestFilter'
  timeout=45m
  ;;

'core-libs-test')
  device_config=smokeTest
  shards=1
  flaky_attempts=0
  test_targets='filter ch.protonmail.android.uitest.filters.CoreLibraryTestFilter'
  timeout=20m
  ;;
*)
  printf 'Invalid argument, specify a valid test suite.'
  exit 1
esac

echo "Y" | gcloud beta --quiet firebase test android run ../firebase-device-config.yml:"$device_config" \
  --app ../app/build/outputs/apk/dev/debug/app-dev-debug.apk \
  --test ../app/build/outputs/apk/androidTest/dev/debug/app-dev-debug-androidTest.apk \
  --test-targets "$test_targets" \
  --use-orchestrator \
  --environment-variables clearPackageData=true \
  --num-flaky-test-attempts=$flaky_attempts \
  --num-uniform-shards=$shards \
  --timeout "$timeout" \
  --no-auto-google-login
