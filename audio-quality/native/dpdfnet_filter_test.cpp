#include "dpdfnet_filter.h"
#include <algorithm>
#include <cmath>
#include <iostream>
#include <stdexcept>
#include <string>
#include <vector>

static void require(bool condition, const char* message) {
    if (!condition) throw std::runtime_error(message);
}

int main(int argc, char** argv) {
    try {
        require(argc == 2, "Supply the bundled speech-models directory");
        for (int rate : {8000, 16000, 24000, 32000, 44100, 48000}) {
            const int model_rate = rate <= 16000 ? rate : 48000;
            const std::string filename = rate == 8000 ? "dpdfnet8_8khz.onnx" :
                rate == 16000 ? "dpdfnet8.onnx" : "dpdfnet8_48khz_hr.onnx";
            const std::string path = std::string(argv[1]) + "/" + filename;
            earcast::DpdfnetFilter dry(rate, model_rate, 0, path.c_str());
            earcast::DpdfnetFilter gentle(rate, model_rate, 6, path.c_str());
            earcast::DpdfnetFilter strong(rate, model_rate, 12, path.c_str());
            const int size = dry.frame_size();
            std::vector<float> a(size), b(size), c(size);
            const float d6 = std::pow(10.0f, -6.0f / 20);
            const float d12 = std::pow(10.0f, -12.0f / 20);
            const float ratio = (1 - d6) / (1 - d12);
            float peak = 0;
            int peak_index = -1;
            for (int block = 0; block < 25; ++block) {
                std::fill(a.begin(), a.end(), 0.0f);
                if (block == 2) a[0] = 0.1f;
                b = a;
                c = a;
                dry.process(a.data(), size);
                gentle.process(b.data(), size);
                strong.process(c.data(), size);
                for (int i = 0; i < size; ++i) {
                    require(std::isfinite(a[i]) && std::isfinite(b[i]) && std::isfinite(c[i]), "Nonfinite output");
                    const float expected = ratio * c[i] + (d6 - ratio * d12) * a[i];
                    require(std::abs(b[i] - expected) < 1e-5f, "Streaming strength is not an aligned linear mix");
                    if (std::abs(a[i]) > peak) { peak = std::abs(a[i]); peak_index = block * size + i; }
                }
            }
            require(peak > 0.01f, "Dry reference impulse disappeared");
            require(std::abs(peak_index - 2 * size - dry.delay_samples()) <= 1, "Incorrect adapter dry delay");
        }
        std::cout << "DPDFNet adapter checks passed; these are not intelligibility scores\n";
        return 0;
    } catch (const std::exception& e) {
        std::cerr << e.what() << '\n';
        return 1;
    }
}
