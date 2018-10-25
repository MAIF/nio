#!/bin/sh

LOCATION=`pwd`

build_schemas () {
  sh $LOCATION/scripts/schemas.sh
}

clean () {
  rm -rf $LOCATION/manual/target/paradox
  rm -rf $LOCATION/docs/manual
}

build () {
  cd $LOCATION/manual
  sbt ';clean;paradox'
  cp -r $LOCATION/manual/target/paradox/site/main $LOCATION/docs
  mv $LOCATION/docs/main $LOCATION/docs/manual
}

case "${1}" in
  all)
    build_schemas
    clean
    build
    ;;
  build_schemas)
    build_schemas
    ;;
  clean)
    clean
    ;;
  build)
    build
    ;;
  cleanbuild)
    clean
    build
    ;;
  *)
    build_schemas
    clean
    build
esac

exit ${?}
