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
  cd $LOCATION
}

build_provider () {
  cd $LOCATION/nio-provider/javascript
  yarn install
  yarn build

  cd $LOCATION
  sbt 'project nio-provider' ';clean;compile;dist;assembly'
}

build_manual () {
  rm -rf $LOCATION/docs/manual

  cd $LOCATION/manual
  sbt ';clean;paradox'
  cp -r $LOCATION/manual/target/paradox/site/main $LOCATION/docs
  mv $LOCATION/docs/main $LOCATION/docs/manual

  mkdir -p $LOCATION/docs/manual/code
  cp $LOCATION/nio-server/conf/swagger/swagger.json $LOCATION/docs/manual/code/swagger.json
}

build_server () {
  cd $LOCATION
  sbt 'project nio-server' ';clean;compile;dist;assembly'
}

run_test () {
    cd $LOCATION
    sbt test
}

case "${1}" in
  all)
    clean
    build_ui
    build_manual
    build_server
    run_test
    # build_cli
    ;;
  github)
    clean
    build_manual
    build_ui
    build_server
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
    run_test
    # build_cli
esac

exit ${?}