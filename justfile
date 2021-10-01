k8s_namespace := "test-network"
docker_registry := "localhost:5000"

build:
	cd apps/LedgerMessaging; ./gradlew build -x test
	cd apps/TradingEngine; ./gradlew build -x test

docker:
	docker build -t trade-dashboard ./apps/Dashboard
	docker build -f apps/LedgerMessaging/src/main/docker/Dockerfile.jvm -t ledgermessaging apps/LedgerMessaging
	docker build -f apps/TradingEngine/src/main/docker/Dockerfile.jvm -t tradeengine apps/TradingEngine
	docker tag trade-dashboard {{docker_registry}}/trade-dashboard:latest
	docker tag ledgermessaging {{docker_registry}}/ledgermessaging:latest
	docker tag tradeengine {{docker_registry}}/tradeengine:latest
	docker push {{docker_registry}}/trade-dashboard:latest
	docker push {{docker_registry}}/ledgermessaging:latest
	docker push {{docker_registry}}/tradeengine:latest

k8s:
	kubectl -n {{k8s_namespace}} apply -f k8s-deployment/trade-dashboard.yaml
	kubectl -n {{k8s_namespace}} apply -f k8s-deployment/trade-engine.yaml
	kubectl -n {{k8s_namespace}} apply -f k8s-deployment/ledger-messaging.yaml
	kubectl -n {{k8s_namespace}} rollout status deploy/trade-engine
	kubectl -n {{k8s_namespace}} rollout status deploy/ledger-messaging
	kubectl -n {{k8s_namespace}} rollout status deploy/trade-dashboard
