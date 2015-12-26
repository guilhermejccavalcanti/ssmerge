#!/bin/bash -e
cd ..
git fetch origin && git reset --hard origin/master
$SHELL