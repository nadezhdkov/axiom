# ==============================================================================
# PROJECT MAKEFILE
#
# This Makefile serves as a wrapper for Gradle commands to streamline the
# development, testing, and deployment processes.
# ==============================================================================

# ------------------------------------------------------------------------------
# VARIABLES
# ------------------------------------------------------------------------------

# The Gradle wrapper executable
GRADLEW = ./gradlew

# Default flags (can be overridden via command line, e.g., make test ARGS="--info")
ARGS ?=

# ------------------------------------------------------------------------------
# CONFIGURATION
# ------------------------------------------------------------------------------

# .PHONY defines targets that are not files. This prevents conflicts if a file
# with the same name as a target is created.
.PHONY: help all publish local clean test build daemon audit-deps

# Default target executed when running 'make' without arguments
default: help

# ------------------------------------------------------------------------------
# TARGETS
# ------------------------------------------------------------------------------

## Display this help message
help:
	@echo "Usage: make [target] [ARGS=\"...\"]"
	@echo ""
	@echo "Targets:"
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^## (.*)/); \
		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "  %-20s %s\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)

## Run clean and publish to remote repository
all: clean publish

## Publish the project to the remote repository
publish: wrapper-check
	@echo "🚀 Publishing to remote repository..."
	@$(GRADLEW) publish $(ARGS)

## Publish the project to the local Maven repository (useful for local testing)
local: wrapper-check
	@echo "📦 Publishing to Maven Local..."
	@$(GRADLEW) publishToMavenLocal $(ARGS)

## Clean the build directory
clean: wrapper-check
	@echo "🧹 Cleaning project..."
	@$(GRADLEW) clean $(ARGS)

## Build the project (compiles and runs checks)
build: wrapper-check
	@echo "🔨 Building project..."
	@$(GRADLEW) build $(ARGS)

## Clean and rebuild the project from scratch
rebuild: clean build

## Run the test suite
test: wrapper-check
	@echo "🧪 Running tests..."
	@$(GRADLEW) test $(ARGS)

## Run classes compilation without daemon (useful for CI/CD or limited memory)
daemon: wrapper-check
	@echo "⚙️  Running Gradle without daemon..."
	@$(GRADLEW) classes --no-daemon -q $(ARGS)

## Verify every declared libs.versions.toml dependency has a real import in src/main
audit-deps:
	@echo "🔎 Auditing declared dependencies against real imports..."
	@bash scripts/audit-deps.sh

# ------------------------------------------------------------------------------
# INTERNAL HELPER TARGETS
# ------------------------------------------------------------------------------

# Ensures the gradlew script is executable
wrapper-check:
	@chmod +x $(GRADLEW)

# End of Makefile
