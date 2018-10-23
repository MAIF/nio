#!/usr/bin/env bash

LOCATION=`pwd`

clean () {
  cd $LOCATION
  rm -rf $LOCATION/nio-server/target/universal
  rm -rf $LOCATION/manual/target/universal
  rm -rf $LOCATION/docs/manual
}

build_ui () {
  cd $LOCATION/nio-server/javascript
  yarn install
  yarn build
}

build_provider () {
  cd $LOCATION/nio-provider/javascript
  yarn install
  yarn build

  cd $LOCATION
  sbt 'project nio-provider' ';clean;compile;dist;assembly'
}

build_manual () {
  cd $LOCATION/manual
  sbt ';clean;paradox'
  cp -r $LOCATION/manual/target/paradox/site/main $LOCATION/docs
  mv $LOCATION/docs/main $LOCATION/docs/manual
}

build_server () {
  cd $LOCATION
  sbt 'project nio-server' ';clean;compile;dist;assembly'
}


case "${1}" in
  all)
    clean
    build_ui
    build_manual
    build_server
    # test_server
    # build_cli
    ;;
  cli)
    build_cli
    ;;
  ui)
    build_ui
    ;;
  clean)
    clean
    ;;
  manual)
    build_manual
    ;;
  server)
    build_server
    ;;
  *)
    clean
    build_ui
    build_manual
    build_server
    build_provider
    # test_server
    # build_cli
esac

exit ${?}