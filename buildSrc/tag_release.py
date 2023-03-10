import os
import re
import subprocess

config_file_path = '../buildSrc/src/main/kotlin/Config.kt'

# Read Config.kt file content
with open(config_file_path, 'r') as file :
  config = file.read()

# Parse version name from config file
version_name_start_index = config.find('versionName')
version_name_end_index = config.find('\n', version_name_start_index)
version_name = config[version_name_start_index:version_name_end_index].split('=')[1].replace('"', '').strip()

# Parse version code from config file
version_code_start_index = config.find('versionCode')
version_code_end_index = config.find('\n', version_code_start_index)
version_code = config[version_code_start_index:version_code_end_index].split('=')[1].strip()

tag = f"{version_name}({version_code})"

# Tag the commit
git_tag_bash_command = f"git tag {tag}"
git_push_tag_bash_command = "git push --tags"
subprocess.Popen(git_tag_bash_command.split(), stdout=subprocess.PIPE)
subprocess.Popen(git_push_tag_bash_command.split(), stdout=subprocess.PIPE)

print(f"Tagged commit with {tag}")
