SHELL=/bin/bash

CB_SPIDER_COMMIT_HASH="7c75d80e8d275d7c137b33a22b2107829eff91b1"

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
