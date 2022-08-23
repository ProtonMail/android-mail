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

# Install gcloud client if not already installed
which gcloud
if [ $? != 0 ]; then
  echo "Installing gcloud client"
  wget --quiet --output-document=/tmp/google-cloud-sdk.tar.gz https://dl.google.com/dl/cloudsdk/channels/rapid/google-cloud-sdk.tar.gz
  mkdir -p /opt
  tar zxf /tmp/google-cloud-sdk.tar.gz --directory /opt
  /opt/google-cloud-sdk/install.sh --quiet
  source /opt/google-cloud-sdk/path.bash.inc
  gcloud components update
fi

# Prepares gcloud client to interact with ProtonMail firebase project
gcloud config set project $CLOUD_PROJECT_ID_MAIL
echo $SERVICE_ACCOUNT_MAIL > /tmp/service-account.json
gcloud auth activate-service-account --key-file /tmp/service-account.json
