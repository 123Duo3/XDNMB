#!/bin/sh
set -eu

pbxproj="appleApp/appleApp.xcodeproj/project.pbxproj"

if [ ! -f "$pbxproj" ]; then
  echo "Missing $pbxproj" >&2
  exit 1
fi

perl -0pi -e 's/DEVELOPMENT_TEAM = [A-Z0-9]{10};/DEVELOPMENT_TEAM = "\$\(TEAM_ID\)";/g' "$pbxproj"
perl -0pi -e 's/\n\s+"PRODUCT_BUNDLE_IDENTIFIER\[sdk=iphoneos\*\]" = [^;]+;//g' "$pbxproj"
