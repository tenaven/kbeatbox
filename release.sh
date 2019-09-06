#!/bin/bash
mkdir target/
TARNAME=target/kbeatbox-$(git log -1 | head -n1 | grep "commit" | cut -c 8-18).tar.gz
tar cvzf $TARNAME src/ README.md pom.xml TODO.md kbeatbox-doc.odp *.puml *.svg
echo "TAR has been created in " $TARNAME