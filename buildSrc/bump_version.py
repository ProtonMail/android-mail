import os
import re

config_file_path = '../buildSrc/src/main/kotlin/Config.kt'
new_version_code = f"versionCode = {os.environ['CI_PIPELINE_IID']}"
print(f"Increasing version code to {new_version_code}")

# Read in the file
with open(config_file_path, 'r') as file :
  config = file.read()

# Replace the target string
config = re.sub(r"versionCode.+", new_version_code, config)

# Write the file out again
with open(config_file_path, 'w') as file:
  file.write(config)

