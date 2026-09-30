i:
    sudo apt-get update
    sudo apt-get install -y build-essential cmake openjdk-17-jdk python3 python3-pip python3-venv
    python3 -m venv .venv
    .venv/bin/python -m pip install -r audio-quality/requirements.txt

build:
    bash ./gradlew build

test:
    bash ./gradlew test

# Build one smaller, complete APK per CPU architecture; every audio feature is included.
build-prod:
    bash ./gradlew :app:assembleLocalRelease -PearcastLocalRelease=true -PearcastSplitApks=true

# Larger APK supporting all four CPU architectures in one file.
build-prod-universal:
    bash ./gradlew :app:assembleLocalRelease -PearcastLocalRelease=true -PearcastSplitApks=false

# Explicit opt-in to Google test banners; normal and release builds stay ad-free.
build-test-ads:
    bash ./gradlew :app:assembleDebug -PearcastTestAds=true

# Size breakdown for the most recently built release APKs.
apk-size:
    python3 scripts/apk_size.py
