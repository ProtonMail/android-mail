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

source "$(dirname "${BASH_SOURCE[0]}")/prelude.sh"

VERSION="$VERSION_NAME($VERSION_CODE)"

printf "Removing SSH remote...\n"
git remote remove origin || true

printf "Setting up HTTPS remote...\n"
git remote add origin https://$GITLAB_USER:$GITLAB_PAT_GIT_HTTPS@$CI_SERVER_HOST/$CI_PROJECT_PATH.git

printf "Tagging version -> '%s'...\n" "$VERSION"

if ! git tag $VERSION; then
  printf "Unable to tag version '%s'. Is the format of the tag correct?.\n" "$VERSION"
  exit 1
fi

git push origin $VERSION
