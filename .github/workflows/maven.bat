@ECHO OFF

git clone -b maven https://%GITHUB_TOKEN%@github.com/ZekerZhayard/ForgeWrapper.git .\maven

xcopy ".\build\maven\" ".\maven\" /S /Y
cd ".\maven\"

git config --local user.name "GitHub Actions"
git config --local user.email "actions@github.com"

git add .
git commit -m "%GITHUB_SHA%"
git push https://%GITHUB_TOKEN%@github.com/ZekerZhayard/ForgeWrapper.git maven
