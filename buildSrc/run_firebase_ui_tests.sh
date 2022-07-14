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

gcloud --quiet firebase test android run ../firebase-device-config.yml:quickTest \
  --app ../app/build/outputs/apk/dev/debug/app-dev-debug.apk \
  --test ../app/build/outputs/apk/androidTest/dev/debug/app-dev-debug-androidTest.apk \
  --test-targets "class ch.protonmail.android.uitest.suite.SmokeSuite" \
  --use-orchestrator \
  --environment-variables clearPackageData=true \
  --num-flaky-test-attempts=1 \
  --timeout 30m
