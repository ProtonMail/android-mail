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

gcloud firebase test android run \
  --app ../app/build/outputs/apk/dev/debug/app-dev-debug.apk \
  --test ../app/build/outputs/apk/androidTest/dev/debug/app-dev-debug-androidTest.apk \
  --type=instrumentation \
  --device model=Pixel2.arm,version=28 \
  --test-targets "notPackage ch.protonmail.android.uitest" \
  --use-orchestrator \
  --num-flaky-test-attempts=1 \
  --timeout 10m \
  --no-auto-google-login
