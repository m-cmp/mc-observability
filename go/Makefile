SHELL=/bin/bash

CB_SPIDER_COMMIT_HASH="7c75d80e8d275d7c137b33a22b2107829eff91b1"

default:
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
