#!/bin/bash
read -p "You will need: jira admin, github permissions on all jbosstm/ repo and nexus permissions. Do you have these?" ENVOK
if [[ $ENVOK == n* ]]
then
  exit
fi

command -v mvn >/dev/null 2>&1 || { echo >&2 "I require mvn but it's not installed.  Aborting."; exit 1; }

if [ $# -eq 0 ]; then
  . scripts/pre-release-vars.sh
  CURRENT=`echo $CURRENT_SNAPSHOT_VERSION | sed "s/-SNAPSHOT//"`
  NEXT=`echo $CURRENT_SNAPSHOT_VERSION | sed "s/.Final//"`
  NEXT="${NEXT%.*}.$((${NEXT##*.}+1))".Final
elif [ $# -lt 2 ]; then
  echo 1>&2 "$0: not enough arguments: CURRENT NEXT (versions should end in .Final or similar)"
  exit 2
elif [ $# -gt 2 ]; then
  echo 1>&2 "$0: too many arguments: CURRENT NEXT (versions should end in .Final or similar)"
  exit 2
else
  CURRENT=$1
  NEXT=$2
fi

set +e
git fetch upstream --tags
git tag | grep $CURRENT
if [[ $? != 0 ]]
then
  set -e
  git checkout 5.5
  set +e
  git status | grep "nothing to commit, working directory is clean"
  if [[ $? != 0 ]]
  then
    git status
    exit
  fi
  git status | grep "ahead"
  if [[ $? != 0 ]]
  then
    git status
    exit
  fi
  set -e
  git log -n 10
  echo Mark version as released in Jira and create next version: https://issues.jboss.org/plugins/servlet/project-config/JBTM/versions
  echo Make sure you have the credentials in your .m2/settings.xml and ignore an error in the final module about missing javadocs
  echo Watch out for sed -i "" in the pre-release.sh as it is does not work on Cygwin
  read -p "Did the log before look OK?" ok
  if [[ $ok == n* ]]
  then
    exit
  else
    ok=y
  fi
  (cd ./scripts/ ; ./pre-release.sh $CURRENT $NEXT)
else
  set -e
  ok=y
fi

if [[ $ok == y* ]]
then
  git fetch upstream
  git checkout $CURRENT
  git clean -f -d
  # Add -x (this will delete all files (e.g. IDE, new features) not under source control)
  mvn clean deploy -Prelease -Dmaven.javadoc.skip=true -DskipTests -pl :narayana-jts-idlj,:jbossxts,:narayana-jts-integration,:byteman_support,:jbosstxbridge,:compensations,:txframework,:restat-integration,:restat-bridge,:restat-api,:restat-util,:restat,:rest-tx -am
fi