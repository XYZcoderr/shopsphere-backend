.PHONY: infra-up infra-down test test-gateway test-order test-inventory
infra-up:
	docker compose up -d postgres kafka
infra-down:
	docker compose down
test: test-gateway test-order test-inventory
test-gateway:
	mvn -f gateway/pom.xml test
test-order:
	mvn -f services/order-service/pom.xml test
test-inventory:
	mvn -f services/inventory-service/pom.xml test
