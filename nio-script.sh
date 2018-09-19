#!/bin/bash

if [ "$1" = "build" ]
then
	echo "Build Nio"

	BUILD_DIRECTORY=$2
	if [ -z "$2" ]
	then
		echo "No build directory specified, we use a new build directory \"nio-build\""
		BUILD_DIRECTORY="nio-build"
	fi 

	sbt 'project nio-server' 'dist'
	
	if [ -d "$BUILD_DIRECTORY" ]
	then
		echo "An existing \"$BUILD_DIRECTORY\" folder already exist, it's time to say good bye!"
		rm -rf $BUILD_DIRECTORY	
	fi

	mkdir -p $BUILD_DIRECTORY
	cp nio-server/target/universal/nio-server-*.zip $BUILD_DIRECTORY
	
	mkdir -p $BUILD_DIRECTORY/javascript 
	cp -r nio-server/javascript $BUILD_DIRECTORY/javascript 
	rm -rf $BUILD_DIRECTORY/javascript/node_modules
	
	cp Dockerfile $BUILD_DIRECTORY
	cd $BUILD_DIRECTORY
	
	docker build -t maif/nio .

elif [ "$1" = "run" ]
then
	echo "Run Nio"

	BUILD_DIRECTORY=$2
	if [ -z "$2" ]
	then
		echo "No run directory specified, we use a new run directory \"nio-run\""
		BUILD_DIRECTORY="nio-run"
	fi
	
	if [ -d "$BUILD_DIRECTORY" ] 
	then
		echo "An existing \"$BUILD_DIRECTORY\" folder already exist, it's time to say good bye!"		
		sudo rm -rf $BUILD_DIRECTORY/data
		sudo rm -rf $BUILD_DIRECTORY/config
		
		docker-compose -f $BUILD_DIRECTORY/docker-compose.yml down
		
		sudo rm -rf $BUILD_DIRECTORY
		echo "it's clean!"
	fi 

	mkdir -p $BUILD_DIRECTORY;
	cd $BUILD_DIRECTORY;

	cp ../docker-compose.prod.yml .
	mv docker-compose.prod.yml docker-compose.yml

	cp -R ../config .

	mkdir -p data/es-data
	echo "All folder and files copied"

	sudo sysctl -w vm.max_map_count=262144

	docker-compose up
elif [ "$1" = "cleanRun" ] && [ -d "$2" ]
then
	echo "Clean run folder"
	
	sudo rm -rf $2/data
	sudo rm -rf $2/config
	
	docker-compose -f $2/docker-compose.yml down
	
	sudo rm -rf $2
	echo "it's clean!"
elif [ "$1" = "cleanBuild" ] && [ -d "$2" ]
then
	echo "Clean build folder"
	sudo rm -rf $2
	echo "it's clean!"

elif [ "$1" = "cleanBuild" ] && [ ! -d "$2" ]
then
	echo "Nothing to clean or build directory not specified"
elif [ "$1" = "cleanRun" ] && [ ! -d "$2" ]
then
	echo "Nothing to clean or run directory not specified"
else 
	echo "Please specify if you want to \"build\", \"run\", \"cleanRun\" or \"cleanBuild\" Nio"
fi

