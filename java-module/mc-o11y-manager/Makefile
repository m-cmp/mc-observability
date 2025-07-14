SHELL = /bin/bash

MANAGER := "mc-o11y-manager"
PID_FILE_MANAGER := $(MANAGER).pid

.PHONY: all build stop run_jar run run_docker stop stop_docker clean help

all: build

build: ## Build jar files
	@echo Building...
	@sh gradlew build -x test
	@git diff > .diff_last_build
	@git rev-parse HEAD > .git_hash_last_build
	@echo Build finished!

stop: ## Stop the manager
	@echo "Stopping $(MANAGER)..."
	@if [ -f "$(PID_FILE_MANAGER)" ]; then \
		PID=$$(cat $(PID_FILE_MANAGER)); \
		echo "Stopping $(MANAGER) process with PID: $$PID"; \
		kill $$PID && rm -f $(PID_FILE_MANAGER); \
		if [ $$? -eq 0 ]; then \
			echo "$(MANAGER)($$PID) has been terminated."; \
		else \
			echo "Failed to terminate $(MANAGER)($$PID)."; \
		fi; \
	else \
		echo "$(MANAGER) PID file does not exist."; \
	fi

run_jar: ## Run built jar
	@git diff > .diff_current
	@STATUS=`diff .diff_last_build .diff_current 2>&1 > /dev/null; echo $$?` && \
	  GIT_HASH_MINE=`git rev-parse HEAD` && \
	  GIT_HASH_LAST_BUILD=`cat .git_hash_last_build 2>&1 > /dev/null | true` && \
	  if [ "$$STATUS" != "0" ] || [ "$$GIT_HASH_MINE" != "$$GIT_HASH_LAST_BUILD" ]; then \
	    "$(MAKE)" build; \
	  fi
	@java -jar build/libs/$(MANAGER).jar

run: stop run_jar ## Stop and run built jar

run_docker: ## Run jar within Docker
	@git diff > .diff_current
	@STATUS=`diff .diff_last_build .diff_current 2>&1 > /dev/null; echo $$?` && \
	  GIT_HASH_MINE=`git rev-parse HEAD` && \
	  GIT_HASH_LAST_BUILD=`cat .git_hash_last_build 2>&1 > /dev/null | true` && \
	  if [ "$$STATUS" != "0" ] || [ "$$GIT_HASH_MINE" != "$$GIT_HASH_LAST_BUILD" ]; then \
	    "$(MAKE)" build; \
	    docker compose build --no-cache; \
	  fi
	@docker compose up -d

stop_docker: ## Stop Docker containers
	@docker compose down

build_docker: build
	@docker compose build --no-cache
	@docker save mc-o11y-manager:latest -o mc-o11y-manager_latest.tar
	@xz -9 -T0 -v mc-o11y-manager_latest.tar

swag: stop build ## Generate Swagger doc YAML file
	@echo Generating Swagger doc YAML file...
	@sh gradlew generateSwaggerYaml
	@sh gradlew stopSpringBoot
	@echo Generating Swagger doc finished!

clean: ## Remove previous build
	@echo Cleaning build...
	@rm -rf .gradle build

help: ## Display this help screen
	@grep -h -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
