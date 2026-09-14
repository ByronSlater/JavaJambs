.DEFAULT_GOAL := help

MVNW := ./mvnw
APP_NAME := cher
SPRING_PROFILES := dev
TEST_PROFILE := test
DEV_DB := cher_development
TEST_DB := cher_test

.PHONY: help clean test verify package install compile run spring-run check

help: ## Show available commands
	@grep -E '^[a-zA-Z_-]+:.*## ' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*## "}; {printf "%-14s %s\n", $$1, $$2}'

clean: ## Remove compiled artifacts
	$(MVNW) clean

compile: ## Compile the application
	$(MVNW) compile

test: ## Run the test suite
	$(MVNW) test -Ddotenv.file=.env.test

verify: ## Run tests and all verification checks
	$(MVNW) verify

package: ## Build the application JAR with minified Tailwind CSS
	$(MVNW) package

install: ## Build and install the artifact locally
	$(MVNW) install

run: ## Run Spring Boot and watch Tailwind CSS
	npx concurrently --kill-others \
		-n "tailwind,spring" \
		-c "cyan,green" \
		"npm run watch:css" \
		"${MVNW} spring-boot:run"

db-up: ## Creates the test and dev dbs locally for the app
	createdb ${DEV_DB} 2>/dev/null || true
	createdb ${TEST_DB} 2>/dev/null || true

db-down: ## Drops the test and dev dbs locally
	dropdb ${DEV_DB} 2>/dev/null || true
	dropdb ${TEST_DB} 2>/dev/null || true

migrate: ## Runs the migrations files against the db using flyway
	$(MVNW) flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/${DEV_DB}
	$(MVNW) flyway:migrate -Dflyway.url=jdbc:postgresql://localhost:5432/${TEST_DB}


full-clean: db-down clean db-up migrate ## Drops the dbs, re-ups them, and runs migrations against them

spring-run: run ## Alias for run

check: ## Clean and run the full verification build
	$(MVNW) clean verify
