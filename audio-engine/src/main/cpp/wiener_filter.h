#pragma once

#include <memory>
#include <vector>

namespace earcast {

// Streaming, single-channel decision-directed Wiener suppression. Each session
// owns its noise estimate and overlap buffers. No allocations in process().
class WienerFilter {
public:
    WienerFilter(int rate, int suppression_db);
    int frame_size() const { return hop_; }
    int delay_samples() const { return hop_; }
    void process(float* samples, int count);

private:
    void estimate_gains();
    int hop_;
    float floor_;
    int frames_ = 0;
    std::unique_ptr<void, void (*)(void*)> fft_;
    std::vector<float> window_, history_, overlap_, time_, spectrum_;
    std::vector<float> power_, smooth_, minimum_, previous_minimum_, noise_;
    std::vector<float> posterior_, gain_, target_;
};

} // namespace earcast
