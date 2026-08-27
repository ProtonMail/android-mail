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

set -e

CONFIG_FILE_PATH="$(git rev-parse --show-toplevel)/app-configuration.properties"

# Offset to align with V6 version code (prod is ~9000 ahead)
VERSION_CODE_OFFSET=9000

CURRENT_VERSION_CODE=$(cat "$CONFIG_FILE_PATH" | grep versionCode | cut -d "=" -f 2)

# A versionCode other than the '1' placeholder means it has already been pinned in the
# configuration file, so it takes precedence over the pipeline based one.
if [ "$CURRENT_VERSION_CODE" != "1" ]; then
  VERSION_CODE=$CURRENT_VERSION_CODE
else
  VERSION_CODE=$((CI_PIPELINE_IID + VERSION_CODE_OFFSET))
fi

VERSION_NAME=$(cat $CONFIG_FILE_PATH | grep versionName | cut -d "=" -f 2 | sed 's/"//g')
