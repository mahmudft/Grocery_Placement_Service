.PHONY: up down infra run test build logs

up:        ## Build and start everything (app + Postgres + Redis + Kafka)
	docker compose up --build -d
	@echo "Swagger UI:  http://localhost:8080/swagger-ui.html"
	@echo "Health:      http://localhost:8080/actuator/health"

down:      ## Stop and remove containers and volumes
	docker compose down -v

infra:     ## Start only the backing services (for running the app from your IDE)
	docker compose up -d postgres redis kafka

run: infra ## Run the app on the host against the compose infrastructure, with readable logs
	SPRING_PROFILES_ACTIVE=plain mvn spring-boot:run

test:      ## Unit + integration tests (needs Docker for Testcontainers)
	mvn -B verify

build:     ## Package the jar
	mvn -B -DskipTests package

logs:
	docker compose logs -f app
