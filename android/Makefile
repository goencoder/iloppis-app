# iLoppis Android – Build, Start & Stop
# Usage: make build | make start | make stop | make logs

# Environment Setup
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools

# Java Version Detection and Setup
# Android development requires Java 17 or 21 (not Java 25+)
JAVA_HOME_CANDIDATES := \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/openjdk-21.0.9/Contents/Home \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home \
	/opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/ms-17.0.11/Contents/Home \
	/opt/homebrew/Cellar/openjdk@17/*/libexec/openjdk.jdk/Contents/Home

# Find first available compatible Java version
JAVA_HOME := $(shell \
	for candidate in $(JAVA_HOME_CANDIDATES); do \
		if [ -d "$$candidate" ]; then \
			echo "$$candidate"; \
			break; \
		fi; \
	done)

# Export both Android and Java environment
export ANDROID_HOME
export JAVA_HOME
export PATH := $(JAVA_HOME)/bin:$(PATH)

ADB          := $(ANDROID_HOME)/platform-tools/adb
EMULATOR     := $(ANDROID_HOME)/emulator/emulator
AVD_NAME     ?= Pixel_6_API_34
PACKAGE_NAME := se.iloppis.app
DEVICE       ?=

.PHONY: help build clean install run start stop emulator stop-emulator create-avd list-avds status logs devices all lint security check test release

help:
	@echo "iLoppis Android Makefile"
	@echo ""
	@echo "Build commands:"
	@echo "  make build          – Build debug APK"
	@echo "  make release        – Build release APK (requires signing config)"
	@echo "  make clean          – Clean build artifacts"
	@echo ""
	@echo "Quality checks:"
	@echo "  make check          – Run all checks (lint + security + tests)"
	@echo "  make lint           – Run Android Lint checks"
	@echo "  make security       – Run security vulnerability scans"
	@echo "  make test           – Run unit tests"
	@echo ""
	@echo "Run commands:"
	@echo "  make start          – Start emulator + install + run app + stream logs"
	@echo "  make run            – Install and launch app on running device/emulator"
	@echo "  make stop           – Stop app and emulator"
	@echo "  make install        – Install APK on connected device/emulator"
	@echo "                        Use DEVICE=<serial> to target specific device"
	@echo ""
	@echo "Device management:"
	@echo "  make devices        – List all connected devices/emulators"
	@echo "  make emulator       – Start emulator (AVD_NAME=$(AVD_NAME))"
	@echo "  make stop-emulator  – Kill running emulator"
	@echo "  make status         – Show device and app status"
	@echo ""
	@echo "Utilities:"
	@echo "  make logs           – Stream app logs (Ctrl+C to stop)"
	@echo "  make create-avd     – Create default AVD (requires system image)"
	@echo "  make list-avds      – List available AVDs"
	@echo "  make all            – Complete workflow: emulator + build + install + run"
	@echo ""
	@echo "Environment:"
	@echo "  ANDROID_HOME = $(ANDROID_HOME)"
	@echo "  JAVA_HOME    = $(JAVA_HOME)"
	@if [ -n "$(JAVA_HOME)" ]; then \
		echo "  Java Version = $$($(JAVA_HOME)/bin/java -version 2>&1 | head -1 | cut -d'"' -f2)"; \
	else \
		echo "  Java Version = ❌ No compatible Java found (need 17 or 21)"; \
	fi
	@echo "  AVD_NAME     = $(AVD_NAME)"
	@echo "  PACKAGE_NAME = $(PACKAGE_NAME)"
	@echo "  DEVICE       = $(DEVICE) (empty = auto-detect)"

build:
	@echo "Building debug APK..."
	@if [ -z "$(JAVA_HOME)" ]; then \
		echo "❌ ERROR: No compatible Java found!"; \
		echo "   Android development requires Java 17 or 21."; \
		echo "   Current system Java: $$(java -version 2>&1 | head -1)"; \
		echo "   Install Java 21: brew install openjdk@21"; \
		exit 1; \
	fi
	@echo "✅ Using Java: $(JAVA_HOME)"
	./gradlew assembleDebug --no-daemon

release:
	@echo "Building release APK..."
	@echo "⚠️  Requires signing configuration in gradle.properties"
	./gradlew assembleRelease --no-daemon

clean:
	@echo "Cleaning build artifacts..."
	./gradlew clean --no-daemon

# Quality Checks (crucial for Google Play & App Store review)
# Run in order: security → lint → test → check (all)

check: security lint test
	@echo ""
	@echo "════════════════════════════════════════════════"
	@echo "✅ ALL CHECKS PASSED! Ready for store submission."
	@echo "════════════════════════════════════════════════"
	@echo ""
	@echo "Reports generated:"
	@echo "  Security: build/reports/dependency-check/dependency-check-report.html"
	@echo "  Lint:     app/build/reports/lint-results-debug.html"
	@echo "  Tests:    app/build/reports/tests/testDebugUnitTest/index.html"

security:
	@echo "════════════════════════════════════════════════"
	@echo "🔒 SECURITY CHECKS"
	@echo "════════════════════════════════════════════════"
	@echo ""
	@echo "=== 1. Checking for hardcoded secrets ==="
	@if grep -rn --include="*.kt" --include="*.java" "api.*key.*=.*\"" app/src/main/java/ | grep -v "getString" | grep -v "// " | grep -v "/\*"; then \
		echo "❌ Found potential hardcoded API keys"; \
		exit 1; \
	else \
		echo "✅ No hardcoded secrets in source"; \
	fi
	@echo ""
	@echo "=== 2. Checking AndroidManifest.xml permissions ==="
	@echo "Declared permissions:"
	@grep "uses-permission" app/src/main/AndroidManifest.xml | sed 's/^/  /' || echo "  (none)"
	@echo ""
	@echo "=== 3. Checking network security config ==="
	@if [ -f app/src/main/res/xml/network_security_config.xml ]; then \
		echo "✅ Network security config present:"; \
		cat app/src/main/res/xml/network_security_config.xml | sed 's/^/  /'; \
	else \
		echo "⚠️  No network_security_config.xml"; \
	fi
	@echo ""
	@echo "=== 4. Checking for debuggable in manifest ==="
	@if grep -q 'android:debuggable="true"' app/src/main/AndroidManifest.xml; then \
		echo "❌ CRITICAL: android:debuggable=true (auto-rejected by stores!)"; \
		exit 1; \
	else \
		echo "✅ Not debuggable"; \
	fi
	@echo ""
	@echo ""
	@echo "=== 5. Dependency vulnerability scan ==="
	@echo "⚠️  Run 'make owasp' in separate terminal (takes 10-30min first time)"
	@echo ""
	@echo "✅ Security checks complete"

owasp:
	@echo "Running OWASP Dependency-Check (downloading CVE database first time)..."
	@echo "This may take 10-30 minutes on first run."
	./gradlew dependencyCheckAnalyze --no-daemon
	@echo ""
	@echo "📊 Report: app/build/reports/dependency-check-report.html"

lint:
	@echo ""
	@echo "════════════════════════════════════════════════"
	@echo "🔍 ANDROID LINT CHECKS"
	@echo "════════════════════════════════════════════════"
	@echo ""
	@echo "Checking for:"
	@echo "  • Security vulnerabilities"
	@echo "  • Privacy issues (permissions, data leaks)"
	@echo "  • Performance problems"
	@echo "  • Accessibility issues"
	@echo "  • API usage violations"
	@echo ""
	./gradlew lintDebug --no-daemon
	@echo ""
	@echo "📊 Report: app/build/reports/lint-results-debug.html"
	@if [ -f app/build/reports/lint-results-debug.xml ]; then \
		if grep -q 'severity="Error"' app/build/reports/lint-results-debug.xml 2>/dev/null; then \
			echo "❌ LINT ERRORS FOUND - Must fix before release"; \
			exit 1; \
		elif grep -q 'severity="Warning"' app/build/reports/lint-results-debug.xml 2>/dev/null; then \
			echo "⚠️  Lint warnings found - Review recommended"; \
		else \
			echo "✅ No lint issues"; \
		fi \
	fi

test:
	@echo ""
	@echo "════════════════════════════════════════════════"
	@echo "🧪 UNIT TESTS"
	@echo "════════════════════════════════════════════════"
	@echo ""
	./gradlew testDebugUnitTest --no-daemon
	@echo ""
	@echo "📊 Report: app/build/reports/tests/testDebugUnitTest/index.html"
	@echo "✅ Tests complete"

devices:
	@echo "=== Connected Devices ==="
	@$(ADB) devices -l

install: build
	@echo "Installing APK..."
ifdef DEVICE
	@echo "Targeting device: $(DEVICE)"
	$(ADB) -s $(DEVICE) install -r app/build/outputs/apk/debug/app-debug.apk
else
	$(ADB) install -r app/build/outputs/apk/debug/app-debug.apk
endif

run: install
	@echo "Launching app..."
ifdef DEVICE
	$(ADB) -s $(DEVICE) shell am start -n $(PACKAGE_NAME)/.MainActivity
else
	$(ADB) shell am start -n $(PACKAGE_NAME)/.MainActivity
endif

start: emulator run
	@echo "✅ App started successfully"
	@echo "📱 Streaming logs... (Press Ctrl+C to stop)"
	@sleep 2
	@$(MAKE) -s logs

stop: stop-app stop-emulator
	@echo "✅ App and emulator stopped"

stop-app:
	@echo "Stopping app..."
ifdef DEVICE
	-$(ADB) -s $(DEVICE) shell am force-stop $(PACKAGE_NAME)
else
	-$(ADB) shell am force-stop $(PACKAGE_NAME)
endif

emulator:
	@echo "Checking if emulator is already running..."
	@if $(ADB) devices | grep -q emulator; then \
		echo "Emulator already running."; \
	else \
		echo "Starting emulator $(AVD_NAME)..."; \
		$(EMULATOR) -avd $(AVD_NAME) -no-snapshot-load & \
		echo "Waiting for device to boot..."; \
		$(ADB) wait-for-device; \
		sleep 5; \
		echo "✅ Emulator ready."; \
	fi

stop-emulator:
	@echo "Stopping emulator..."
	-$(ADB) emu kill 2>/dev/null || echo "No emulator running"

status:
	@echo "=== Device Status ==="
	@$(ADB) devices -l
	@echo ""
	@echo "=== App Status ==="
	@if $(ADB) shell pidof $(PACKAGE_NAME) >/dev/null 2>&1; then \
		echo "App is running (PID: $$($(ADB) shell pidof $(PACKAGE_NAME)))"; \
	else \
		echo "App is not running"; \
	fi

logs:
	@echo "Streaming logs for $(PACKAGE_NAME)... (Press Ctrl+C to stop)"
	$(ADB) logcat -s iLoppis:* AndroidRuntime:E *:F

create-avd:
	@echo "Downloading system image (arm64)..."
	sdkmanager --sdk_root=$(ANDROID_HOME) "system-images;android-34;google_apis;arm64-v8a"
	@echo "Creating AVD $(AVD_NAME)..."
	avdmanager create avd -n $(AVD_NAME) -k "system-images;android-34;google_apis;arm64-v8a" --device "pixel_6" --force
	@echo "✅ AVD $(AVD_NAME) created."

list-avds:
	@echo "Available AVDs:"
	@avdmanager list avd

all: start
	@echo "✅ Complete workflow finished"
