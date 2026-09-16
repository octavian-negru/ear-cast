# Full DPDFNet8 models

These are actual pretrained streaming ONNX models, not placeholder graphs.
They run through the complete sherpa-onnx v1.13.8 C API. All three use eight
dual-path blocks; rate-specific models avoid pretending a narrowband microphone
contains full-band speech.

| Capture PCM rate | Model | Origin |
|---|---|---|
| 8 kHz | `dpdfnet8_8khz.onnx` | [CEVA model](https://huggingface.co/Ceva-IP/DPDFNet/blob/main/onnx/dpdfnet8_8khz.onnx) |
| 16 kHz | `dpdfnet8.onnx` | [sherpa-onnx export](https://github.com/k2-fsa/sherpa-onnx/releases/download/speech-enhancement-models/dpdfnet8.onnx) |
| 24/32/44.1/48 kHz | `dpdfnet8_48khz_hr.onnx` | [CEVA model](https://huggingface.co/Ceva-IP/DPDFNet/blob/main/onnx/dpdfnet8_48khz_hr.onnx) |

Hashes in `SHA256SUMS` were compared with upstream release/Hugging Face LFS
digests. The Android loader verifies the selected model before use. SpeexDSP
quality-10 conversion handles capture rates different from the model rate.

License: Apache-2.0, `LICENSE-DPDFNet`. [Research and source](https://github.com/ceva-ip/DPDFNet).
Source inspected at commit `9bd9844a227bb6aa57e55588d8d0e961fcff1c46`.
The 48 kHz file calls its metadata profile `dpdfnet2_48khz_hr` despite the eight-block
filename/weights; that existing upstream profile is supported by the runtime.
Weights and metadata are preserved byte-for-byte.

The exported `before_df` path delays the spectrum twice in `Mask`, then uses the
centre of the five-frame deep-filter history (another two hops). Streaming
analysis/synthesis adds a further hop. Our declared delay is therefore five
10 ms hops, plus any resampler delay; the Kotlin adapter adds another hop.
This is source-derived timing, pending model/device golden-output validation.
The model has no exposed speech-confidence output; the RNNoise-specific quiet
speech leveler is not applied to it.
