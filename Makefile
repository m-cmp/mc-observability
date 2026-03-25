SHELL = /bin/bash

MANAGER := "mc-o11y-manager"
PID_FILE_MANAGER := $(MANAGER).pid

.PHONY: all build stop swag swag-java swag-python swag-merge clean help

all: build

build: ## Build jar files
	@echo Building...
	@cd java && sh gradlew build -x test -x check -x spotlessCheck -x spotlessApply
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

swag: swag-java swag-python swag-merge ## Generate Swagger doc YAML file
	@rm -f swagger/java-swagger.yaml swagger/python-swagger.yaml
	@echo Swagger doc generated at swagger/swagger.yaml

swag-java:
	@echo Generating Java Swagger...
	@mkdir -p swagger
	@cd java && sh gradlew generateSwaggerYaml -x spotlessCheck -x spotlessApply

swag-python:
	@echo Generating Python Swagger...
	@mkdir -p swagger
	@cd python/insight && PYTHONPATH=. uv run python3 ../../scripts/extract_python_swagger.py

swag-merge:
	@echo Merging Java + Python Swagger...
	@cd python/insight && PYTHONPATH=. uv run python3 ../../scripts/merge_swagger.py

clean: ## Remove previous build
	@echo Cleaning build...
	@cd java && rm -rf .gradle build
	@cd java/mc-o11y-manager && rm -rf .gradle build

help: ## Display this help screen
	@grep -h -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'
