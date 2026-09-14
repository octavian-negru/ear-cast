#include "speech_leveler.h"
#include <algorithm>
#include <cmath>
#include <iostream>
#include <stdexcept>
#include <vector>

static void require(bool value, const char* message) {
    if (!value) throw std::runtime_error(message);
}

int main() {
    try {
        std::vector<float> audio(480);
        openhearing::SpeechLeveler disabled(0), active(12), noise_only(12);
        for (int block = 0; block < 200; ++block) {
            std::fill(audio.begin(), audio.end(), 0.001f);
            disabled.process(audio.data(), 480, 1.0f);
            require(audio[0] == 0.001f, "Off must be bit-exact");
            active.process(audio.data(), 480, 1.0f);
            require(audio[0] >= 0.001f && audio.back() <= 0.003982f, "Gain must remain in bounds");
        }
        require(active.gain() > 3.5f, "Quiet confident speech should receive meaningful gain");
        for (int block = 0; block < 200; ++block) {
            std::fill(audio.begin(), audio.end(), 0.001f);
            noise_only.process(audio.data(), 480, 0.1f);
            require(audio[0] == 0.001f, "Noise alone must not cause gain growth or gating");
            active.process(audio.data(), 480, 0.1f);
            require(audio[0] >= 0.001f, "Loss of confidence must not gate input");
        }
        require(active.gain() < 1.01f, "Added gain must relax after speech ends");
        for (int block = 0; block < 200; ++block) {
            std::fill(audio.begin(), audio.end(), 0.001f);
            active.process(audio.data(), 480, 1.0f);
        }
        std::fill(audio.begin(), audio.end(), 0.001f);
        audio[240] = 0.75f;
        active.process(audio.data(), 480, 1.0f);
        require(audio[240] <= 0.80001f, "Sudden peaks must constrain existing gain immediately");
        std::cout << "Speech leveler checks passed\n";
        return 0;
    } catch (const std::exception& error) {
        std::cerr << error.what() << '\n';
        return 1;
    }
}
