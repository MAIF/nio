# Nio


## start MongoDB and Kafka

```bash
sudo docker-compose up
```

## build and start

Start backend 

```bash
sbt ~run
```

Start frontend 

```bash
cd javascript && yarn install;
yarn start
```

## Access to app

Access to app on your browser : [http://localhost:9000/sandbox/bo](http://localhost:9000/sandbox/bo)


## Published Kafka Events

| Event Type           	               | Description                                                    	| Payload example                                                                                                                                                                              	|
|------------------------------------- |-------------------------------------------------------------------	|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------	|
| TenantCreated        	               | Emitted when a new tenant has been created.                        | ```{"type":"TenantCreated","author":"tenant-admin","date":"2018-04-11T12:27:34Z","id":984045289865216000,"payload":{"key":"newTenant","description":"a new tenant"}}```  |
| TenantDeleted        	               | Emitted when an existing tenant has been deleted.                  | ```{"type":"TenantDeleted","author":"tenant-admin","date":"2018-04-11T12:27:35Z","id":984045289865216004,"payload":{"key":"newTenant","description":"a new tenant"}}```  |
| OrganisationCreated  	               | Emitted when a new Organisation has been created (as a draft). 	| ```{"type":"OrganisationCreated","tenant":"prod","author":"admin@test.com","date":"2018-04-11T12:27:41Z","id":984045319233732614,"payload":{"key":"org1","label":"lbl1","version":{"status":"DRAFT","num":1,"latest":false},"groups":[{"key":"group1","label":"blalba","permissions":[{"key":"sms","label":"Please accept sms"}]}]}}```                                   	                    |
| OrganisationUpdated  	               | Emitted when an existing Organisation has been updated         	| ```{"type":"OrganisationUpdated","tenant":"prod","author":"admin@test.com","date":"2018-01-01T20:20:20Z","id":984045319233732615,"payload":{"key":"org1","label":"lbl2","version":{"status":"DRAFT","num":1,"latest":false},"groups":[{"key":"group1","label":"blalba","permissions":[{"key":"sms","label":"Please accept sms"}]}]}, "oldValue": {"key":"org1","label":"lbl1","version":{"status":"DRAFT","num":1,"latest":false},"groups":[{"key":"group1","label":"blalba","permissions":[{"key":"sms","label":"Please accept sms"}]}]}} ``` 	                            |
| OrganisationReleased 	               | Emitted when an existing Organisation draft has been released. 	| ```{"type":"OrganisationReleased","tenant":"prod","author":"admin@test.com","date":"2018-04-11T12:27:41Z","id":984045319397310471,"payload":{"key":"org1","label":"lbl2","version":{"status":"RELEASED","num":1,"latest":true},"groups":[{"key":"group1","label":"blalba","permissions":[{"key":"sms","label":"Please accept sms"}]}]}}```                                                               	|
| OrganisationDeleted  	               | Emitted when an existing Organisation has been deleted.       	    | ```{"type":"OrganisationDeleted","tenant":"prod","author":"admin@test.com","date":"2018-04-11T12:27:42Z","id":984045319233732615,"payload":{"key":"org1","label":"lbl2","version":{"status":"RELEASED","num":1,"latest":true},"groups":[{"key":"group1","label":"blalba","permissions":[{"key":"sms","label":"Please accept sms"}]}]}}```                                   	                    |
| ConsentFactCreated   	               | Emitted when a new ConsentFact has been created.               	| ```{"type":"ConsentFactCreated","tenant":"prod","author":"admin@test.com",date":"2018-04-11T12:27:43Z","id":984045319397310472,"payload":{"userId":"toto1","doneBy":{"userId":"bob","role":"admin"},"version":1,"groups":[{"key":"group1","label":"blabla","consents":[{"key":"sms","label":"Please accept sms","checked":true}]}], "orgKey":"org1" }```                                                         	|
| ConsentFactUpdated   	               | Emitted when an existing ConsentFact has been updated.         	| ```{"type":"ConsentFactUreated","tenant":"prod","author":"admin@test.com",date":"2018-04-11T12:27:44Z","id":984045319397310473,"payload":{"userId":"toto1","doneBy":{"userId":"bob","role":"admin"},"version":1,"groups":[{"key":"group1","label":"blabla","consents":[{"key":"sms","label":"Please accept sms","checked":true}]}], "orgKey":"org1" }, "oldValue":{"userId":"toto1","doneBy":{"userId":"bob","role":"admin"},"version":1,"groups":[{"key":"group1","label":"blabla","consents":[{"key":"sms","label":"Please accept sms","checked":true}]}], "orgKey":"org1" } ```                      	|
| AccountCreated   	                   | Emitted when a new Account has been created.         	            | ```{"type":"AccountCreated","tenant":"prod","author":"admin@test.com","account":"account1","date":"2018-04-25T13:22:54Z","id":989132643281928192,"payload":{"accountId":"account1","creationDate":"2018-04-25T13:22:45Z","organisationsUsers":[{"userId":"user1","orgKey":"org1"},{"userId":"user1","orgKey":"org2"}]}} ```                      	|
| AccountUpdated   	                   | Emitted when an existing Account has been updated.         	    | ```{"type":"AccountUpdated","tenant":"prod","author":"admin@test.com","account":"account1","date":"2018-04-25T13:22:54Z","id":989132644422778881,"payload":{"accountId":"account1","creationDate":"2018-04-25T13:22:45Z","organisationsUsers":[{"userId":"user1","orgKey":"org1"},{"userId":"user1","orgKey":"org2"},{"userId":"user1","orgKey":"org3"}]},"oldValue":{"accountId":"account1","creationDate":"2018-04-25T13:22:45Z","organisationsUsers":[{"userId":"user1","orgKey":"org1"},{"userId":"user1","orgKey":"org2"}]}} ```                      	|
| AccountDeleted   	                   | Emitted when an existing Account has been deleted.         	    | ```{"type":"AccountDeleted","tenant":"prod","author":"admin@test.com","account":"account1","date":"2018-04-25T13:22:54Z","id":989132645030952962,"payload":{"accountId":"account1","creationDate":"2018-04-25T13:22:45Z","organisationsUsers":[{"userId":"user1","orgKey":"org1"},{"userId":"user1","orgKey":"org2"},{"userId":"user1","orgKey":"org3"}]}} ```                      	|
| DeletionStarted                      | Emitted when a DeletionTask is started.                   	        | ```{"type":"DeletionStarted","tenant":"prod","date":"2018-04-25T13:22:54Z","id":989132645030952962,"payload":{"orgKey":"maif","userId":"toto","appId": "nestor1", "deletionTaskId":"delTaskMaif1"}]}} ```                      	|
| DeletionAppDone                      | Emitted when an app has finished it's deletion.             	    | ```{"type":"DeletionAppDone","tenant":"prod","date":"2018-04-25T13:22:54Z","id":989132645030952963,"payload":{"id": "abc",orgKey":"maif","userId":"toto","startedAt":"2018-04-25T13:22:54Z","appIds":["nestor1", "socialClub1"],"states":[{"appId":"nestor1","status":"Running"},{"appId":"socialClub1","Done"}],"status":"Running","lastUpdate":"2018-04-25T13:22:45Z"}]},"oldValue":{"id": "abc",orgKey":"maif","userId":"toto","startedAt":"2018-04-25T13:22:54Z","appIds":["nestor1", "socialClub1"],"states":[{"appId":"nestor1","status":"Running"},{"appId":"socialClub1","Running"}],"status":"Running","lastUpdate":"2018-04-25T13:22:45Z"}]}} ```                      	|
| DeletionFinished                     | Emitted when a DeletionTask is finished.                           | ```{"type":"DeletionFinished","tenant":"prod","date":"2018-04-25T13:22:54Z","id":989132645030952963,"payload":{"id": "abc",orgKey":"maif","userId":"toto","startedAt":"2018-04-25T13:22:54Z","appIds":["nestor1", "socialClub1"],"states":[{"appId":"nestor1","status":"Done"},{"appId":"socialClub1","Done"}],"status":"Done","lastUpdate":"2018-04-25T13:22:45Z"}]}} ```                      	|
| ExtractionStarted                    | Emitted when an ExtractionTask is started.                   	    | ```{"type":"ExtractionStarted","tenant":"sandbox","author":"test@test.com","date":"2018-06-04T13:05:13Z","id":1003623711494897664,"payload":{"orgKey":"orgTest1","userId":"toto","appId":"app1","extractionTaskId":"5b1539095f0000255472cdc4"}}```                      	|
| ExtractionAppFilesMetadataReceived   | Emitted when the files metadata was received for an app            | ```{"type":"ExtractionAppFilesMetadataReceived","tenant":"sandbox","author":"test@test.com","date":"2018-06-04T13:05:20Z","id":1003623739412185090,"payload":{"orgKey":"orgTest1","userId":"toto","appId":"app1","files":[{"name":"file_app1_5055686403064275342.json","contentType":"json","size":18}]}}```                      	|
| ExtractionAppDone                    | Emitted when an app has finished it's extraction.           	    | ```{"type":"ExtractionAppDone","tenant":"sandbox","author":"test@test.com","date":"2018-06-04T13:05:27Z","id":1003623770361954308,"payload":{"orgKey":"orgTest1","userId":"toto","appId":"app1"}}```                      	|
| ExtractionFinished                   | Emitted when a ExtractionTask is finished.                         | ```{"type":"ExtractionFinished","tenant":"sandbox","author":"test@test.com","date":"2018-06-04T13:05:31Z","id":1003623783485931526,"payload":{"id":"5b1539095f0000255472cdc4","orgKey":"orgTest1","userId":"toto","startedAt":"2018-06-04T13:05:13Z","appIds":["app1","app2"],"states":[{"appId":"app1","files":[{"name":"file_app1_5055686403064275342.json","contentType":"json","size":18}],"totalBytes":18,"status":"Done"},{"appId":"app2","files":[{"name":"file_app2_6706799341605546550.json","contentType":"json","size":18}],"totalBytes":18,"status":"Done"}],"status":"Done","progress":100,"lastUpdate":"2018-06-04T13:05:31Z","done":2}}```                      	|
