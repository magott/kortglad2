#!/bin/bash

if [ "$#" -lt 1 ]
then
  echo "Missing heroku app name"
  exit 1
fi
echo "building"
sbt stage
echo "pushing"
heroku container:push web --app $1
echo "releasing"
heroku container:release web --app $1
heroku logs --app $1