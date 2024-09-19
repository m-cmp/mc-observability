SHELL=/bin/bash

CB_SPIDER_COMMIT_HASH="9102c6f415a57dd3b422648ca123c0880d9ca56f"

GOPROXY_OPTION := GOPROXY=direct
GO_COMMAND := ${GOPROXY_OPTION} go
GOPATH := $(shell go env GOPATH)

default:
	@echo "[*] Checking swag installation..."
	@if [ ! -f "${GOPATH}/bin/swag" ] && [ ! -f "$(GOROOT)/bin/swag" ]; then \
  	  echo "[*] Installing swag..."; \
	  ${GO_COMMAND} install github.com/swaggo/swag/cmd/swag@latest; \
	fi
	@echo "[*] Cloning CB-Spider..."
	@if [ ! -d "./cb-spider" ]; then \
		  git clone https://github.com/cloud-barista/cb-spider; \
		fi
	@echo "[*] Patching Azure Monitoring feature to CB-Spider..."
	@cd cb-spider && \
		git reset && \
		git checkout -- . && \
		git clean -fd && \
		git fetch origin && \
		git checkout $(CB_SPIDER_COMMIT_HASH) && \
		git reset && \
		git checkout -- . && \
		git clean -fd && \
		patch_files=$$(find ../patches -type f -name '*.patch' | sort) && \
		for patch_file in $${patch_files[@]}; do \
		  patch -p1 < $$patch_file; \
		done
	@echo "[*] Building cb-spider..."
	@make -C cb-spider
	@echo "[*] Linking folders..."
	@mkdir -p meta_db/
	@if [ ! -d "conf" ]; then \
		  cp -RpPf cb-spider/conf .; \
		fi
	@if [ ! -f "setup.env" ]; then \
		  cp -pPf cb-spider/setup.env .; \
		fi
	@mkdir -p log/
	@rm -rf  cb-spider/meta_db && ln -sf ../meta_db cb-spider/meta_db
	@rm -rf  cb-spider/conf && ln -sf ../conf cb-spider/conf
	@rm -rf  cb-spider/log && ln -sf ../log cb-spider/log
	@rm -f  cb-spider/setup.env && ln -sf ../setup.env cb-spider/setup.env

run:
	@cd cb-spider && ./bin/start.sh

stop:
	@cd cb-spider && ./bin/stop.sh
