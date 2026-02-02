#
# Build or debug environment
#
ENV ?= staging



# iLoppis Mobile App
# Root Makefile for Android and iOS development
#
# Quick start:
#   make android-device    Deploy to connected Android phone
#   make android-emulator  Run in Android emulator
#   make ios               Run in iOS simulator

.PHONY: help android-device android-emulator android-build android-clean \
        ios ios-build ios-clean logs-android logs-ios check

# Default target
help:
	@echo ""
	@echo "╔══════════════════════════════════════════════════════════════╗"
	@echo "║              iLoppis Mobile App - Makefile                   ║"
	@echo "╚══════════════════════════════════════════════════════════════╝"
	@echo ""
	@echo "🤖 ANDROID COMMANDS:"
	@echo "  make android-device     Deploy and run on connected phone"
	@echo "  make android-emulator   Start emulator and run app"
	@echo "  make android-build      Build debug APK"
	@echo "  make android-release    Build release APK"
	@echo "  make android-clean      Clean Android build artifacts"
	@echo "  make android-devices    List connected Android devices"
	@echo "  make android-logs       Stream Android app logs"
	@echo "  make android-check      Run lint, security, and tests"
	@echo ""
	@echo "🍎 iOS COMMANDS:"
	@echo "  make ios                Start simulator and run app"
	@echo "  make ios-build          Build for simulator"
	@echo "  make ios-clean          Clean iOS build artifacts"
	@echo "  make ios-devices        List iOS simulators"
	@echo "  make ios-logs           Stream iOS app logs"
	@echo ""
	@echo "📋 UTILITY COMMANDS:"
	@echo "  make check              Run all quality checks (Android)"
	@echo "  make clean              Clean all build artifacts"
	@echo ""
	@echo "📚 API DOCUMENTATION:"
	@echo "  API spec available at: spec/swagger/iloppis.swagger.json"
	@echo ""

# ============================================================================
# ANDROID TARGETS
# ============================================================================

# Deploy to physical Android device
android-device:
	@echo "📱 Deploying $(ENV) to connected Android device..."
	@cd android && ENV=$(ENV) $(MAKE) run

# Run in Android emulator
android-emulator:
	@echo "🖥️  Starting Android emulator with $(ENV) and deploying app..."
	@cd android && ENV=$(ENV) $(MAKE) start

# Build Android APK
android-build:
	@cd android && ENV=$(ENV) $(MAKE) build

# Build release APK
android-release:
	@cd android && ENV=$(ENV) $(MAKE) release

# Clean Android build
android-clean:
	@cd android && $(MAKE) clean

# List Android devices
android-devices:
	@cd android && $(MAKE) devices

# Stream Android logs
android-logs:
	@cd android && $(MAKE) logs

# Run Android quality checks
android-check:
	@cd android && $(MAKE) check

# Stop Android app and emulator
android-stop:
	@cd android && $(MAKE) stop

# ============================================================================
# iOS TARGETS
# ============================================================================

# Run in iOS simulator
ios:
	@echo "🍎 Starting iOS simulator and deploying app..."
	@cd ios && $(MAKE) start

# Build iOS app
ios-build:
	@cd ios && $(MAKE) build

# Clean iOS build
ios-clean:
	@cd ios && $(MAKE) clean

# List iOS simulators
ios-devices:
	@cd ios && $(MAKE) devices

# Stream iOS logs
ios-logs:
	@cd ios && $(MAKE) logs

# Stop iOS app and simulator
ios-stop:
	@cd ios && $(MAKE) stop

# ============================================================================
# COMMON TARGETS
# ============================================================================

# Run all quality checks
check: android-check
	@echo "✅ All checks complete"

# Clean all builds
clean: android-clean ios-clean
	@echo "✅ All build artifacts cleaned"
