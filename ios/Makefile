# iLoppis iOS – Build, Start & Stop
# Usage: make build | make start | make stop | make logs

PROJECT      := iLoppis.xcodeproj
SCHEME       := iLoppis
DEVICE_NAME  ?= iPhone 17
ENV          ?= staging
ifeq ($(ENV),prod)
ENV          := production
endif

ifeq ($(ENV),staging)
BUNDLE_ID     := se.iloppis.app.staging
DISPLAY_NAME  := iLoppis (Staging)
API_BASE_URL  := https://iloppis-staging.fly.dev/
DEBUG_LOGGING := YES
else ifeq ($(ENV),production)
BUNDLE_ID     := se.iloppis.app
DISPLAY_NAME  := iLoppis
API_BASE_URL  := https://iloppis.se/
DEBUG_LOGGING := NO
else
$(error ENV must be staging, production, or prod)
endif

BUILD_DIR    ?= $(CURDIR)/build/DerivedData
XCODE_ENV_SETTINGS := \
	ILOPPIS_ENVIRONMENT=$(ENV) \
	ILOPPIS_API_BASE_URL=$(API_BASE_URL) \
	ILOPPIS_DISPLAY_NAME='$(DISPLAY_NAME)' \
	ILOPPIS_NETWORK_DEBUG_LOGGING=$(DEBUG_LOGGING) \
	PRODUCT_BUNDLE_IDENTIFIER=$(BUNDLE_ID)

.PHONY: help build release archive clean install run start stop start-simulator stop-simulator status logs devices screenshot all get-device-id

help:
	@echo "iLoppis iOS Makefile"
	@echo ""
	@echo "Build commands:"
	@echo "  make build          – Build app for simulator"
	@echo "  make release        – Build Release app for simulator"
	@echo "  make archive        – Create a device archive (signing required)"
	@echo "  make clean          – Clean build artifacts"
	@echo ""
	@echo "Run commands:"
	@echo "  make start          – Start simulator + build + run app + stream logs"
	@echo "  make run            – Build and launch app on running simulator"
	@echo "  make stop           – Stop app and simulator"
	@echo "  make install        – Build and install app (without launching)"
	@echo ""
	@echo "Simulator management:"
	@echo "  make start-simulator – Boot simulator (DEVICE_NAME=$(DEVICE_NAME))"
	@echo "  make stop-simulator  – Shutdown simulator"
	@echo "  make status          – Show simulator and app status"
	@echo "  make devices         – List available iOS simulators"
	@echo ""
	@echo "Utilities:"
	@echo "  make logs           – Stream app logs (Ctrl+C to stop)"
	@echo "  make screenshot FILE=<output.png> – Capture the selected simulator"
	@echo "  make all            – Complete workflow: simulator + build + run"
	@echo ""
	@echo "Environment:"
	@echo "  DEVICE_NAME = $(DEVICE_NAME)"
	@echo "  BUNDLE_ID   = $(BUNDLE_ID)"
	@echo "  ENV         = $(ENV)"
	@echo "  API_BASE_URL = $(API_BASE_URL)"

get-device-id:
	@xcrun simctl list devices available | grep "$(DEVICE_NAME) (" | head -1 | sed -E 's/.*\(([A-F0-9-]+)\).*/\1/'

build:
	@echo "Building $(SCHEME) for simulator..."
	xcodebuild -project $(PROJECT) \
		-scheme $(SCHEME) \
		-configuration Debug \
		-destination 'platform=iOS Simulator,name=$(DEVICE_NAME)' \
		-derivedDataPath $(BUILD_DIR) \
		$(XCODE_ENV_SETTINGS) \
		build

release:
	@echo "Building $(SCHEME) $(ENV) Release for simulator..."
	xcodebuild -project $(PROJECT) \
		-scheme $(SCHEME) \
		-configuration Release \
		-destination 'platform=iOS Simulator,name=$(DEVICE_NAME)' \
		-derivedDataPath $(BUILD_DIR) \
		$(XCODE_ENV_SETTINGS) \
		build

archive:
	@echo "Archiving $(SCHEME) $(ENV) for device..."
	xcodebuild -project $(PROJECT) \
		-scheme $(SCHEME) \
		-configuration Release \
		-destination 'generic/platform=iOS' \
		-archivePath build/$(SCHEME)-$(ENV).xcarchive \
		$(XCODE_ENV_SETTINGS) \
		archive

clean:
	@echo "Cleaning build artifacts..."
	xcodebuild -project $(PROJECT) -scheme $(SCHEME) -derivedDataPath $(BUILD_DIR) clean

install: build
	@echo "Installing app on simulator..."
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -z "$$DEVICE_ID" ]; then \
		echo "❌ Error: Device '$(DEVICE_NAME)' not found. Run 'make devices' to see available devices."; \
		exit 1; \
	fi; \
	xcrun simctl install $$DEVICE_ID \
		$$(find $(BUILD_DIR) -name "$(SCHEME).app" -type d | head -1); \
	echo "✅ App installed"

run: install
	@echo "Launching app..."
	@DEVICE_ID=$$(make -s get-device-id); \
	xcrun simctl launch $$DEVICE_ID $(BUNDLE_ID)
	@echo "✅ App launched"

start: start-simulator run
	@echo "✅ App started successfully"
	@echo "📱 Streaming logs... (Press Ctrl+C to stop)"
	@sleep 2
	@$(MAKE) -s logs

stop: stop-app stop-simulator
	@echo "✅ App and simulator stopped"

stop-app:
	@echo "Stopping app..."
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -n "$$DEVICE_ID" ]; then \
		xcrun simctl terminate $$DEVICE_ID $(BUNDLE_ID) 2>/dev/null || echo "App not running"; \
	fi

start-simulator:
	@echo "Starting simulator $(DEVICE_NAME)..."
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -z "$$DEVICE_ID" ]; then \
		echo "❌ Error: Device '$(DEVICE_NAME)' not found. Run 'make devices' to see available devices."; \
		exit 1; \
	fi; \
	open -a Simulator; \
	xcrun simctl boot $$DEVICE_ID 2>/dev/null || echo "Simulator already booted"; \
	xcrun simctl bootstatus $$DEVICE_ID -b; \
	echo "✅ Simulator ready"

stop-simulator:
	@echo "Shutting down simulator..."
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -n "$$DEVICE_ID" ]; then \
		xcrun simctl shutdown $$DEVICE_ID 2>/dev/null || echo "Simulator not running"; \
	fi

status:
	@echo "=== Simulator Status ==="
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -n "$$DEVICE_ID" ]; then \
		xcrun simctl list devices | grep "iPhone 17 " | grep -v "Pro"; \
	else \
		echo "Device '$(DEVICE_NAME)' not found"; \
	fi
	@echo ""
	@echo "=== App Status ==="
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -n "$$DEVICE_ID" ]; then \
		if xcrun simctl listapps $$DEVICE_ID 2>/dev/null | grep -q "$(BUNDLE_ID)"; then \
			echo "App is installed"; \
			if ps aux | grep -v grep | grep -q "$(BUNDLE_ID)"; then \
				echo "App is running"; \
			else \
				echo "App is not running"; \
			fi; \
		else \
			echo "App is not installed"; \
		fi; \
	fi

logs:
	@echo "Streaming logs for $(BUNDLE_ID)... (Press Ctrl+C to stop)"
	@DEVICE_ID=$$(make -s get-device-id); \
	if [ -z "$$DEVICE_ID" ]; then \
		echo "❌ Error: Device '$(DEVICE_NAME)' not found"; \
		exit 1; \
	fi; \
	xcrun simctl spawn $$DEVICE_ID log stream \
		--style compact \
		--predicate '(process == "iLoppis" OR subsystem CONTAINS "se.iloppis")'

devices:
	@echo "Available iOS Simulators:"
	@xcrun simctl list devices available | grep -E "iPhone|iPad" | sed 's/^/  /'

screenshot:
	@if [ -z "$(FILE)" ]; then \
		echo "Usage: make screenshot FILE=<output.png> [DEVICE_NAME=\"<device>\"]"; \
		exit 1; \
	fi
	@DEVICE_ID=$$($(MAKE) -s get-device-id); \
	if [ -z "$$DEVICE_ID" ]; then \
		echo "❌ Error: Device '$(DEVICE_NAME)' not found"; \
		exit 1; \
	fi; \
	mkdir -p "$$(dirname "$(FILE)")"; \
	xcrun simctl io $$DEVICE_ID screenshot "$(FILE)"; \
	echo "✅ Screenshot saved: $(FILE)"

all: start
	@echo "✅ Complete workflow finished"
