


: ${DOCKER_TAG:=ledgerable-event-contract}

docker build -t ${DOCKER_TAG} .

# create the connection.json 
