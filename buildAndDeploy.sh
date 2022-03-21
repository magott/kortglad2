#!/bin/bash
set -e
echo "Starting deploy"
if [ "$#" -lt 1 ]
then
  echo "Missing heroku app name"
  exit 1
fi
if [[ "$1" = "kortglad" ]]
then
  tag="prod"
elif [[ "$1" = "kortglad-stage" ]]
then
  tag="stage"
else
  echo "Invalid app name ($1), must be kortglad or kortglad-stage"
fi

echo "Checking for uncomitted changes"
git update-index --refresh
git diff-index --quiet HEAD -- || echo "Uncommitted changes, aborting"

echo "Logging into heroku container"
heroku container:login

echo "Building"
sbt stage
echo "Pushing container to heroku"
heroku container:push web --app $1
echo "Releasing"
heroku container:release web --app $1

echo "Tagging successful release with $tag"
git push origin :refs/tags/${tag}
git tag -fa ${tag} -m "Heroku deploy to $1"
git push origin main ${tag}

heroku logs --app $1