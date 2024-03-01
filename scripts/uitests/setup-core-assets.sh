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
set -o pipefail
set -o errexit

PROJECT_ROOT_PATH=$(git rev-parse --show-toplevel)

echo PROXY_TOKEN="$(curl -o - https://proxy.proton.black/token/get)" >> $PROJECT_ROOT_PATH/private.properties

mkdir $PROJECT_ROOT_PATH/app/src/uiTest/assets -p
base64 -d - < "$TEST_USERS_CREDENTIALS_FILE" > $PROJECT_ROOT_PATH/app/src/uiTest/assets/users.json
base64 -d - < "$INTERNAL_API_FILE" > $PROJECT_ROOT_PATH/app/src/uiTest/assets/internal_api.json
