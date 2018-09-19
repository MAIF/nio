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

	if [ -d "$BUILD_DIRECTORY" ]
	then
		echo "An existing \"$BUILD_DIRECTORY\" folder already exist, it's time to say good bye!"
		rm -rf $BUILD_DIRECTORY	
	fi

	cd nio-server/javascript
	echo "Installing Yarn"
	sudo npm install -g yarn
	yarn install
	echo "Running JS build"
	yarn run build
	echo "Destroying dependencies cache"
	rm -rf ./node_modules

	cd ../..

	pwd
	
	sbt 'project nio-server' 'dist'
	
	mkdir -p $BUILD_DIRECTORY
	cp nio-server/target/universal/nio-server-*.zip $BUILD_DIRECTORY	
	cp Dockerfile $BUILD_DIRECTORY

	cd $BUILD_DIRECTORY
	pwd

	docker build -t maif/nio .

elif [ "$1" = "run" ]
then
	echo "DO NOT FORGET to setup your hosts"
	echo "/etc/hosts: 127.0.0.1    nio.foo.bar kibana.foo.bar elastic.foo.bar keycloak.foo.bar otoroshi.foo.bar otoroshi-api.foo.bar privateapps.foo.bar"

	echo "DO NOT FORGET to replace the ip address a line 144 in docker-compose.prod.yml by your ip address"
	echo "credentials are the following"

	echo "http://keycloak.foo.bar:8889 : keycloak/password"
	echo "http://otoroshi.foo.bar:8889 : admin@otoroshi.io/password"
	echo "http://kibana.foo.bar:8889 : index on otoroshi-events-* / @timestamp"
	echo "http://nio.foo.bar:8889 : admin@nio.io/password"

	read -p "have you setup your host and add your ip address in docker-compose.prod.yml [press y to continue] ? " -n 1 -r
	echo    # (optional) move to a new line
	if [[ $REPLY =~ ^[Yy]$ ]]
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
	fi
elif [ "$1" = "cleanRun" ] && [ -d "$2" ]
then
	echo "Clean run folder"
	
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

