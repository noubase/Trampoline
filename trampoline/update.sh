#!/usr/bin/env bash

git pull;
mvn clean package -Dmaven.test.skip=true
sudo systemctl restart trampoline
sudo journalctl -fxe -u trampoline