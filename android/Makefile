# iLoppis Android – Build, Start & Stop
# Usage: make build | make start | make stop | make logs

# Environment Setup
ENV ?= staging
ifeq ($(ENV),prod)
ENV := production
endif
FLAVOR_CAPITALIZED := $(shell echo $(ENV) | awk '{print toupper(substr($$0,1,1)) substr($$0,2)}')
ANDROID_HOME ?= /opt/homebrew/share/android-commandlinetools
EXPECTED_APP_ID := se.iloppis.app$(if $(filter staging,$(ENV)),.staging)

ifeq ($(ENV),staging)
API_BASE_URL   := https://iloppis-staging.fly.dev/
else ifeq ($(ENV),production)
API_BASE_URL   := https://iloppis.se/
else
$(error ENV must be staging, production, or prod)
endif

BUILD_CONFIG_PATH := app/build/generated/source/buildConfig/$(ENV)/release/se/iloppis/app/BuildConfig.java
MERGED_MANIFEST_PATH := app/build/intermediates/merged_manifest/$(ENV)Release/process$(FLAVOR_CAPITALIZED)ReleaseMainManifest/AndroidManifest.xml
AAB_PATH := app/build/outputs/bundle/$(ENV)Release/app-$(ENV)-release.aab
PLAY_DEPLOYMENT_CERT ?= deployment_cert.der
# Public SHA-256 fingerprint from Play Console. This pins the local public
# deployment/app-signing certificate; it is not an upload signing secret.
EXPECTED_PLAY_DEPLOYMENT_CERT_SHA256 := 2cfb7bfb93009fd2bacfcecffcc9d33423d74d34dd25206469cd76ebd0dff32b

# Java Version Detection and Setup
# Android development requires Java 17 or 21 (not Java 25+)
JAVA_HOME_CANDIDATES := \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/ms-21.0.9/Contents/Home \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/openjdk-21.0.9/Contents/Home \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home \
	/opt/homebrew/Cellar/openjdk@21/*/libexec/openjdk.jdk/Contents/Home \
	/Users/goranengdahl/Library/Java/JavaVirtualMachines/ms-17.0.11/Contents/Home \
	/opt/homebrew/Cellar/openjdk@17/*/libexec/openjdk.jdk/Contents/Home \
	/usr/lib/jvm/java-21-openjdk \
	/usr/lib/jvm/java-17-openjdk

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
PACKAGE_NAME := se.iloppis.app$(if $(filter staging,$(ENV)),.staging)
DEVICE       ?=

.PHONY: help build clean install run start stop emulator stop-emulator create-avd list-avds status logs devices all lint security check test release bundle release-check verify-release-config verify-play-deployment-cert verify-aab-signature

help:
	@echo "iLoppis Android Makefile"
	@echo ""
	@echo "Build commands:"
	@echo "  make build          – Build debug APK"
	@echo "  make release        – Build release APK (requires signing config)"
	@echo "  make bundle         – Build release AAB for store upload"
	@echo "  make release-check  – Run flavored debug unit tests, release lint, manifest verification, and AAB build"
	@echo "  make verify-play-deployment-cert – Verify the local Play deployment certificate"
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
	@echo "Building $(ENV) debug APK..."
	@if [ -z "$(JAVA_HOME)" ]; then \
		echo "❌ ERROR: No compatible Java found!"; \
		echo "   Android development requires Java 17 or 21."; \
		echo "   Current system Java: $$(java -version 2>&1 | head -1)"; \
		echo "   Install Java 21: brew install openjdk@21"; \
		exit 1; \
	fi
	@echo "✅ Using Java: $(JAVA_HOME)"
	./gradlew assemble$(FLAVOR_CAPITALIZED)Debug --no-daemon

release:
	@echo "Building $(ENV) release APK..."
	@echo "⚠️  Requires signing configuration in gradle.properties"
	./gradlew assemble$(FLAVOR_CAPITALIZED)Release --no-daemon

bundle:
	@echo "Building $(ENV) release Android App Bundle..."
	./gradlew bundle$(FLAVOR_CAPITALIZED)Release --no-daemon

release-check:
	@echo "Running $(ENV) release quality gate..."
	./gradlew \
		test$(FLAVOR_CAPITALIZED)DebugUnitTest \
		lint$(FLAVOR_CAPITALIZED)Release \
		bundle$(FLAVOR_CAPITALIZED)Release \
		--no-daemon
	@$(MAKE) verify-release-config ENV=$(ENV)
	@$(MAKE) verify-play-deployment-cert ENV=$(ENV)
	@$(MAKE) verify-aab-signature ENV=$(ENV)
	@echo "✅ $(ENV) release quality gate passed"
	@echo "📦 AAB: $(AAB_PATH)"

verify-release-config:
	@echo "Verifying $(ENV) release manifest and BuildConfig..."
	@test -f $(BUILD_CONFIG_PATH) || (echo "❌ Missing BuildConfig: $(BUILD_CONFIG_PATH)" && exit 1)
	@test -f $(MERGED_MANIFEST_PATH) || (echo "❌ Missing merged manifest: $(MERGED_MANIFEST_PATH)" && exit 1)
	@grep -q 'public static final boolean DEBUG = false;' $(BUILD_CONFIG_PATH) || (echo "❌ Release BuildConfig is still debuggable" && exit 1)
	@grep -q 'public static final String APPLICATION_ID = "$(EXPECTED_APP_ID)";' $(BUILD_CONFIG_PATH) || (echo "❌ APPLICATION_ID mismatch in BuildConfig" && exit 1)
	@grep -q 'public static final String API_BASE_URL = "$(API_BASE_URL)";' $(BUILD_CONFIG_PATH) || (echo "❌ API_BASE_URL mismatch in BuildConfig" && exit 1)
	@grep -q 'public static final String APP_ENVIRONMENT = "$(ENV)";' $(BUILD_CONFIG_PATH) || (echo "❌ APP_ENVIRONMENT mismatch in BuildConfig" && exit 1)
	@grep -q 'package="$(EXPECTED_APP_ID)"' $(MERGED_MANIFEST_PATH) || (echo "❌ Package mismatch in merged manifest" && exit 1)
	@if grep -q 'android:debuggable="true"' $(MERGED_MANIFEST_PATH); then \
		echo "❌ Release manifest is debuggable"; \
		exit 1; \
	fi
	@echo "✅ Release BuildConfig and merged manifest verified"

verify-play-deployment-cert:
	@if [ "$(ENV)" != "production" ]; then \
		echo "ℹ️  Play deployment certificate verification is only required for production"; \
		exit 0; \
	fi; \
	test -f "$(PLAY_DEPLOYMENT_CERT)" || (echo "❌ Missing Play deployment certificate: $(PLAY_DEPLOYMENT_CERT)" && exit 1); \
	openssl x509 -inform DER -in "$(PLAY_DEPLOYMENT_CERT)" -noout >/dev/null 2>&1 || (echo "❌ Invalid DER X.509 certificate: $(PLAY_DEPLOYMENT_CERT)" && exit 1); \
	actual_fingerprint=$$(shasum -a 256 "$(PLAY_DEPLOYMENT_CERT)" | awk '{print $$1}'); \
	if [ "$$actual_fingerprint" != "$(EXPECTED_PLAY_DEPLOYMENT_CERT_SHA256)" ]; then \
		echo "❌ Play deployment certificate fingerprint mismatch"; \
		echo "   Expected: $(EXPECTED_PLAY_DEPLOYMENT_CERT_SHA256)"; \
		echo "   Actual:   $$actual_fingerprint"; \
		exit 1; \
	fi; \
	echo "✅ Play deployment certificate verified: $$actual_fingerprint"

verify-aab-signature:
	@test -f "$(AAB_PATH)" || (echo "❌ Missing AAB: $(AAB_PATH)" && exit 1)
	@verification_output=$$("$(JAVA_HOME)/bin/jarsigner" -verify -strict "$(AAB_PATH)" 2>&1); \
	if ! printf '%s\n' "$$verification_output" | grep -q '^jar verified'; then \
		printf '%s\n' "$$verification_output"; \
		echo "❌ AAB is not signed with an upload key"; \
		echo "   deployment_cert.der is public and cannot sign the bundle."; \
		exit 1; \
	fi; \
	echo "✅ AAB signature verified"

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
	@echo "📊 Report: build/reports/dependency-check/dependency-check-report.html"

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
	$(ADB) -s $(DEVICE) install -r app/build/outputs/apk/$(ENV)/debug/app-$(ENV)-debug.apk
else
	$(ADB) install -r app/build/outputs/apk/$(ENV)/debug/app-$(ENV)-debug.apk
endif

run: install
	@echo "Launching app..."
ifdef DEVICE
	$(ADB) -s $(DEVICE) shell am start -n $(PACKAGE_NAME)/se.iloppis.app.MainActivity
else
	$(ADB) shell am start -n $(PACKAGE_NAME)/se.iloppis.app.MainActivity
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
