i:
    sudo apt-get update
    sudo apt-get install -y build-essential cmake openjdk-17-jdk python3 python3-pip python3-venv
    python3 -m venv .venv
    .venv/bin/python -m pip install -r audio-quality/requirements.txt

build:
    bash ./gradlew build

test:
    bash ./gradlew test

# Build an optimized release APK; uses keystore.properties when configured.
build-prod:
    bash ./gradlew :app:assembleRelease
