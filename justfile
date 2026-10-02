i:
    sudo apt-get update
    # The system JDK launches Gradle, which provisions the pinned JDK 27 runtime.
    sudo apt-get install -y build-essential cmake default-jdk python3 python3-pip python3-venv
    python3 -m venv .venv
    .venv/bin/python -m pip install -r audio-quality/requirements.txt

build:
    bash ./gradlew build

test:
    bash ./gradlew test

# Signed release APK per architecture.
build-prod:
    bash ./gradlew :app:assembleLocalRelease -PearcastLocalRelease=true -PearcastSplitApks=true

# Universal release APK.
build-prod-universal:
    bash ./gradlew :app:assembleLocalRelease -PearcastLocalRelease=true -PearcastSplitApks=false

apk-size:
    python3 scripts/apk_size.py
