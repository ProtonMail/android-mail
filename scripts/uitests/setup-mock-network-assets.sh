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

function setup_remote() {
  printf "Setting up mock assets from a remote repository.\n"

  if [ -n "$NETWORK_ASSETS_TARGET_REF" ]; then
    # Override with the lockfile content, if set.
    NETWORK_ASSETS_MAIN_REF=$NETWORK_ASSETS_TARGET_REF
  fi

  if [ -z "$NETWORK_ASSETS_REMOTE_REPOSITORY" ]; then
    printf "No remote set for the network assets repository.\n"
    exit 1
  fi

  if [ -z "$NETWORK_ASSETS_TARGET_REF" ]; then
    printf "No target ref specified, defaulting to the master branch.\n"
    NETWORK_ASSETS_TARGET_REF=master
  fi

  rm -rf "${PROJECT_ROOT_PATH:?}/$NETWORK_ASSETS_PROJECT_PATH"
  mkdir -p "$PROJECT_ROOT_PATH/$NETWORK_ASSETS_PROJECT_PATH" && cd "$_"

  printf "Checking out '%s' at revision '%s'...\n" "$NETWORK_ASSETS_REMOTE_REPOSITORY" "$NETWORK_ASSETS_TARGET_REF"
  if ! git clone "$NETWORK_ASSETS_REMOTE_REPOSITORY" . -q; then
    # There's no need for a custom error message here, as git will return enough details.
    exit 1
  fi

  if ! git checkout "$NETWORK_ASSETS_MAIN_REF" -q; then
    printf "Unable to checkout revision '%s', check your lockfile.\n" "$NETWORK_ASSETS_MAIN_REF"
    exit 1
  fi

  if ! sed -i.bak '/^NETWORK_ASSETS_TARGET_REF=/s~=.*$~='"$(git rev-parse HEAD)"'~' "$SCRIPT_ROOT_PATH"/AssetsFile.lock; then
    printf "Unable to update the lockfile.\n"
    exit 1
  fi

  printf "Checkout succeeded.\n"
}

function setup_local() {
  if [ -z "$NETWORK_ASSETS_LOCAL_PATH" ]; then
    printf "No path defined for local network assets.\n"
    exit 1
  fi

  rm -rf "$NETWORK_ASSETS_PROJECT_PATH" && mkdir -p "$NETWORK_ASSETS_PROJECT_PATH"

  printf "Proceeding with symlinking '%s' to '%s'.\n" "$NETWORK_ASSETS_LOCAL_PATH" "$NETWORK_ASSETS_PROJECT_PATH"
  if ! ln -s "$NETWORK_ASSETS_LOCAL_PATH"/* "$NETWORK_ASSETS_PROJECT_PATH"; then
    printf "Local symlink failed.\n"
    exit 1
  fi

  printf "Local symlink succeeded.\n"
}

PROJECT_ROOT_PATH=$(git rev-parse --show-toplevel)
SCRIPT_ROOT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

source "$SCRIPT_ROOT_PATH"/AssetsFile
source "$SCRIPT_ROOT_PATH"/AssetsFile.lock

case $1 in
'setup-local')
  read -rp "Specify the absolute local path of the assets repository (defaults to '$NETWORK_ASSETS_DEFAULT_LOCAL_PATH'): " CUSTOM_ASSETS_LOCAL_PATH

  if [ -z "$CUSTOM_ASSETS_LOCAL_PATH" ]; then
    printf "Using '%s' as default path.\n" "$NETWORK_ASSETS_DEFAULT_LOCAL_PATH"
    NETWORK_ASSETS_LOCAL_PATH=$(cd "$NETWORK_ASSETS_DEFAULT_LOCAL_PATH" && pwd)
  else
    NETWORK_ASSETS_LOCAL_PATH=$CUSTOM_ASSETS_LOCAL_PATH
  fi

  setup_local
  ;;

'setup-remote')
  setup_remote
  ;;

*)
  printf "Invalid argument passed, use either 'setup-local' or 'setup-remote'."
  ;;
esac
