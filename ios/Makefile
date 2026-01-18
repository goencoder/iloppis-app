# iLoppis iOS – Build, Start & Stop
# Usage: make build | make start | make stop | make logs

PROJECT      := iLoppis.xcodeproj
SCHEME       := iLoppis
DEVICE_NAME  ?= iPhone 17
BUNDLE_ID    := se.iloppis.app
BUILD_DIR    := $(HOME)/Library/Developer/Xcode/DerivedData

.PHONY: help build clean install run start stop start-simulator stop-simulator status logs devices all get-device-id

help:
	@echo "iLoppis iOS Makefile"
	@echo ""
	@echo "Build commands:"
	@echo "  make build          – Build app for simulator"
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
	@echo "  make all            – Complete workflow: simulator + build + run"
	@echo ""
	@echo "Environment:"
	@echo "  DEVICE_NAME = $(DEVICE_NAME)"
	@echo "  BUNDLE_ID   = $(BUNDLE_ID)"

get-device-id:
	@xcrun simctl list devices available | grep "iPhone 17 " | grep -v "Pro" | head -1 | sed -E 's/.*\(([A-F0-9-]+)\).*/\1/'

build:
	@echo "Building $(SCHEME) for simulator..."
	xcodebuild -project $(PROJECT) \
		-scheme $(SCHEME) \
		-destination 'platform=iOS Simulator,name=$(DEVICE_NAME)' \
		build

clean:
	@echo "Cleaning build artifacts..."
	xcodebuild -project $(PROJECT) -scheme $(SCHEME) clean
	rm -rf $(BUILD_DIR)/iLoppis-*

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

all: start
	@echo "✅ Complete workflow finished"
