#!/bin/bash

SERVER_DIR=tmp-wildfly

rm -rf $SERVER_DIR

galleon.sh feature-pack clear-cache 
galleon.sh provision provisioning.xml --dir=$SERVER_DIR
