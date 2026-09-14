#pragma once

namespace openhearing {

// Bounded upward leveling after enhancement. Never gates audio or attenuates
// below unity. Confidence controls gain growth, not whether audio is audible.
class SpeechLeveler {
public:
    explicit SpeechLeveler(float maximum_gain_db);
    void process(float* samples, int count, float speech_probability);
    float gain() const { return gain_; }
private:
    float maximum_gain_;
    float gain_ = 1.0f;
};

}  // namespace openhearing
