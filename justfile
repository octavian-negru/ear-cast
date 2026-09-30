i:
    sudo apt-get update
    sudo apt-get install -y build-essential cmake openjdk-17-jdk python3 python3-pip python3-venv
    python3 -m venv .venv
    .venv/bin/python -m pip install -r audio-quality/requirements.txt

build:
    bash ./gradlew build

test:
    bash ./gradlew test

# Build an installable optimized APK; use the release key, or the local debug key if unconfigured.
build-prod:
    bash ./gradlew :app:assembleLocalRelease -PearcastLocalRelease=true

# Explicit opt-in to Google test banners; normal and release builds stay ad-free.
build-test-ads:
    bash ./gradlew :app:assembleDebug -PearcastTestAds=true
